package de.hhn.gameoflife;

public record Settings(int worldWidth, int worldHeight) {
  public Settings {
    if (worldWidth <= 0) {
      throw new IllegalArgumentException("worldWidth must be greater than 0");
    }
    if (worldHeight <= 0) {
      throw new IllegalArgumentException("worldHeight must be greater than 0");
    }
  }
}