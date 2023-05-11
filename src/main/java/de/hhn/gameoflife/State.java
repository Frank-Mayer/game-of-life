package de.hhn.gameoflife;

public class State<T> {
  public static <T> State<T> useState(final T init) {
    return new State<T>(init);
  }

  private T value;

  public State(final T init) {
    this.value = init;
  }

  public T get() {
    return this.value;
  }

  public void set(final T value) {
    this.value = value;
  }
}