package de.hhn.gameoflife.logic;

/**
 * Shape drawing style categories.
 *
 * <p>Only used for the GUI.
 */
public enum DrawingStyleCategory {
  STILL_LIFES("Still Lifes"),
  OSCILLATORS("Oscillators"),
  SPACESHIPS("Spaceships");

  private final String name;

  DrawingStyleCategory(final String name) {
    this.name = name;
  }

  public String getName() {
    return this.name;
  }
}
