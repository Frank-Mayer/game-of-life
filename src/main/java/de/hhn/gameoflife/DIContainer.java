package de.hhn.gameoflife;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.Callable;

public class DIContainer implements Disposable {

  private static final int MAX_RELATION_DISTANCE = Integer.MAX_VALUE / 2;

  private static int getRelationDistance(final Class<?> from, final Class<?> to) {
    if (from == null || to == null) {
      return DIContainer.MAX_RELATION_DISTANCE;
    }

    if (from.getName().equals(to.getName())) {
      return 0;
    }

    if (!to.isAssignableFrom(from)) {
      return DIContainer.MAX_RELATION_DISTANCE;
    }

    var distance = DIContainer.MAX_RELATION_DISTANCE;
    final var superDistance = DIContainer.getRelationDistance(from.getSuperclass(), to) + 1;
    if (superDistance < distance) {
      distance = superDistance;
    }
    for (final var iface : from.getInterfaces()) {
      final var ifaceDistance = DIContainer.getRelationDistance(iface, to) + 1;
      if (ifaceDistance < distance) {
        distance = ifaceDistance;
      }
    }
    return distance;
  }

  private static boolean typeIsPrimitive(final Class<?> clazz) {
    return clazz.isPrimitive()
        || clazz == Boolean.class
        || clazz == Character.class
        || clazz == Byte.class
        || clazz == Short.class
        || clazz == Integer.class
        || clazz == Long.class
        || clazz == Float.class
        || clazz == Double.class;
  }

  private final Map<Class<?>, Class<?>> unconstructedSingletons = new HashMap<>();
  private final Map<Class<?>, Object> constructedSingletons = new HashMap<>();
  private final Set<Class<?>> transients = new HashSet<>();

  private final Map<Class<?>, Callable<Object>> factories = new HashMap<>();

  private boolean disposed = false;

  public DIContainer() {
    this.addSingleton(this);
  }

  public void dispose() {
    if (this.disposed) {
      return;
    }
    this.disposed = true;
    this.constructedSingletons.values().stream()
        .filter(x -> x instanceof Disposable)
        .forEach(x -> ((Disposable) x).dispose());
    this.unconstructedSingletons.clear();
    this.constructedSingletons.clear();
    this.transients.clear();
    this.factories.clear();
  }

  public <T> T get(final Class<T> clazz) {
    var relationDistance = Integer.MAX_VALUE;
    Callable<Object> getClosestInstance = null;

    // singletons
    for (final var x : this.constructedSingletons.entrySet()) {
      final var distance = DIContainer.getRelationDistance(x.getKey(), clazz);
      if (distance < relationDistance) {
        relationDistance = distance;
        getClosestInstance = x::getValue;
      }
    }
    if (relationDistance == 0 && getClosestInstance != null) {
      try {
        return (T) getClosestInstance.call();
      } catch (final Exception e) {
        throw new RuntimeException(e);
      }
    }

    for (final var x : this.unconstructedSingletons.entrySet()) {
      final var distance = DIContainer.getRelationDistance(x.getKey(), clazz);
      if (distance < relationDistance) {
        relationDistance = distance;
        getClosestInstance =
            () -> {
              final Object instance = this.construct(x.getValue());
              this.constructedSingletons.put(x.getKey(), instance);
              this.unconstructedSingletons.remove(x.getKey());
              return instance;
            };
      }
    }
    if (relationDistance == 0 && getClosestInstance != null) {
      try {
        return (T) getClosestInstance.call();
      } catch (final Exception e) {
        throw new RuntimeException(e);
      }
    }

    // transients
    for (final var x : this.transients) {
      final var distance = DIContainer.getRelationDistance(x, clazz);
      if (distance < relationDistance) {
        relationDistance = distance;
        getClosestInstance = () -> this.construct(x);
      }
    }
    if (relationDistance == 0 && getClosestInstance != null) {
      try {
        return (T) getClosestInstance.call();
      } catch (final Exception e) {
        throw new RuntimeException(e);
      }
    }

    // factories
    for (final var x : this.factories.entrySet()) {
      final var distance = DIContainer.getRelationDistance(x.getKey(), clazz);
      if (distance < relationDistance) {
        relationDistance = distance;
        getClosestInstance = () -> x.getValue().call();
      }
    }

    // check if the class required service is available
    if (relationDistance != DIContainer.MAX_RELATION_DISTANCE && getClosestInstance != null) {
      try {
        return (T) getClosestInstance.call();
      } catch (final Exception e) {
        throw new RuntimeException(e);
      }
    }

    if (DIContainer.typeIsPrimitive(clazz)) {
      System.out.printf("DIContainer: get primitive %s%n", clazz);
      return (T) this.getPrimitiveDefault(clazz);
    }

    if (clazz.isArray()) {
      System.out.printf("DIContainer: get array %s%n", clazz);
      return (T) new Object[0];
    }

    if (clazz.isInterface()
        || Modifier.isAbstract(clazz.getModifiers())
        || clazz.isEnum()
        || clazz.isAnnotation()) {
      throw new RuntimeException(
          String.format(
              "No implementation for non callable type \"%s\" registered", clazz.getName()));
    }

    return (T) this.construct(clazz);
  }

  public void addSingleton(final Class<?> clazz) {
    this.unconstructedSingletons.put(clazz, clazz);
    this.constructedSingletons.remove(clazz);
    this.transients.remove(clazz);
    this.factories.remove(clazz);
  }

  public void addSingleton(final Class<?> clazz, final Class<?> as) {
    if (!as.isAssignableFrom(clazz)) {
      throw new RuntimeException(
          String.format("Class \"%s\" is not assignable to \"%s\"", clazz.getName(), as.getName()));
    }

    this.unconstructedSingletons.put(as, clazz);
    this.constructedSingletons.remove(as);
    this.transients.remove(clazz);
    this.factories.remove(clazz);
  }

  public void addSingleton(final Object instance) {
    final var clazz = instance.getClass();
    this.addSingleton(instance, clazz);
  }

  public void addSingleton(final Object instance, final Class<?> clazz) {
    this.constructedSingletons.put(clazz, instance);
    this.unconstructedSingletons.remove(clazz);
    this.transients.remove(clazz);
    this.factories.remove(clazz);
  }

  public void addTransient(final Class<?> clazz) {
    this.transients.add(clazz);
    this.unconstructedSingletons.remove(clazz);
    this.constructedSingletons.remove(clazz);
    this.factories.remove(clazz);
  }

  public void addFactory(final Class<?> clazz, final Callable<Object> factory) {
    this.factories.put(clazz, factory);
    this.unconstructedSingletons.remove(clazz);
    this.constructedSingletons.remove(clazz);
    this.transients.remove(clazz);
  }

  private Object construct(final Class<?> clazz) {
    final var constr =
        Arrays.stream(clazz.getConstructors())
            .sorted((a, b) -> Integer.compare(a.getParameterCount(), b.getParameterCount()))
            .toArray(Constructor<?>[]::new);

    constructors_loop:
    for (final var constructor : constr) {
      final var params = constructor.getParameterTypes();
      final var args = new Object[params.length];
      for (var i = 0; i < params.length; ++i) {
        try {
          args[i] = this.get(params[i]);
        } catch (final Exception e) {
          continue constructors_loop;
        }
      }
      try {
        return constructor.newInstance(args);
      } catch (final Exception e) {
        System.err.printf("DIContainer: constructor %s throwed: %s%n", constructor, e.getMessage());
      }
    }
    throw new RuntimeException(String.format("Failed to construct \"%s\"", clazz.getName()));
  }

  private Object getPrimitiveDefault(final Class<?> clazz) {
    switch (clazz.getName()) {
      case "boolean":
      case "java.lang.Boolean":
        return false;
      case "char":
      case "java.lang.Character":
        return '\0';
      case "byte":
      case "java.lang.Byte":
        return (byte) 0;
      case "short":
      case "java.lang.Short":
        return (short) 0;
      case "java.lang.String":
        return "";
      case "int":
      case "java.lang.Integer":
        return 0;
      case "long":
      case "java.lang.Long":
        return 0L;
      case "float":
      case "java.lang.Float":
        return 0.0f;
      case "double":
      case "java.lang.Double":
        return 0.0d;
    }
    throw new RuntimeException(String.format("No default value for \"%s\" found", clazz.getName()));
  }
}