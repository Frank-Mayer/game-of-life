package de.hhn.gameoflife;

public interface Drawable<T> {
  void set(final T data);

  void set(final int index, final boolean alife);

  void draw();
}