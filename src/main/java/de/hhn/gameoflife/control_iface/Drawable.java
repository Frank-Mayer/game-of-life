package de.hhn.gameoflife.control_iface;

/** Interface for objects that need to be drawn */
public interface Drawable<T> {
  void set(final T data);

  void set(final int index, final boolean alife);

  void draw();

  void compose();
}