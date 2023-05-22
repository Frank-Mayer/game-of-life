package de.hhn.gameoflife;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

public class DIContainer {
  private final Map<Class<?>, Class<?>> unconstructedSingletons = new HashMap<>();
  private final Map<Class<?>, Object> constructedSingletons = new HashMap<>();
  private final Set<Class<?>> transients = new HashSet<>();
  private final Map<Class<?>, Callable<Object>> factories = new HashMap<>();

  public DIContainer() {
    this.addSingleton(this);
  }

  public <T> T get(final Class<T> clazz) {
    if (this.constructedSingletons.containsKey(clazz)) {
      return (T) this.constructedSingletons.get(clazz);
    }

    if (this.unconstructedSingletons.containsKey(clazz)) {
      final Class<?> impl = this.unconstructedSingletons.get(clazz);
      try {
        final Object instance = this.construct(impl);
        this.constructedSingletons.put(clazz, instance);
        this.unconstructedSingletons.remove(clazz);
        return (T) instance;
      } catch (final Exception e) {
        throw new RuntimeException(e);
      }
    }

    if (this.transients.contains(clazz)) {
      try {
        return (T) this.construct(clazz);
      } catch (final Exception e) {
        throw new RuntimeException(e);
      }
    }

    if (this.factories.containsKey(clazz)) {
      try {
        return (T) this.factories.get(clazz).call();
      } catch (final Exception e) {
        throw new RuntimeException(e);
      }
    }

    if (clazz.isPrimitive()) {
      return (T) this.getPrimitiveDefault(clazz);
    }

    if (clazz.isArray()) {
      return (T) new Object[0];
    }

    throw new RuntimeException(
        String.format("No implementation for \"%s\" registered", clazz.getName()));
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

  public void addSingleton(final Object instance) {
    this.addSingleton(instance, instance.getClass());
  }

  private Object construct(final Class<?> clazz) {
    final var constr =
        Arrays.stream(clazz.getConstructors())
            .sorted((a, b) -> Integer.compare(b.getParameterCount(), a.getParameterCount()))
            .toArray(Constructor<?>[]::new);

    for (final var constructor : constr) {
      try {
        final var params = constructor.getParameterTypes();
        final var args = new Object[params.length];
        for (var i = 0; i < params.length; i++) {
          args[i] = this.get(params[i]);
        }
      } catch (final Throwable ignore) {
        continue;
      }
    }
    throw new RuntimeException(String.format("Failed to construct \"%s\"", clazz.getName()));
  }

  private Object getPrimitiveDefault(final Class<?> clazz) {
    if (clazz == boolean.class) {
      return false;
    }
    if (clazz == byte.class) {
      return (byte) 0;
    }
    if (clazz == short.class) {
      return (short) 0;
    }
    if (clazz == int.class) {
      return 0;
    }
    if (clazz == long.class) {
      return 0L;
    }
    if (clazz == float.class) {
      return 0.0f;
    }
    if (clazz == double.class) {
      return 0.0d;
    }
    if (clazz == char.class) {
      return '\u0000';
    }
    throw new RuntimeException(String.format("No default value for \"%s\" found", clazz.getName()));
  }
}