package de.hhn.gameoflife;

/** Interface for objects that need to be drawn */
public interface Drawable<T> {
  void set(final T data);

  void set(final int index, final boolean alife);

  void draw();
}