package de.hhn.gameoflife.logic;

public record Settings(int worldWidth, int worldHeight) {
  public Settings {
    if (worldWidth <= 0) {
      throw new IllegalArgumentException("worldWidth must be greater than 0");
    }
    if (worldHeight <= 0) {
      throw new IllegalArgumentException("worldHeight must be greater than 0");
    }
    if ((worldWidth & (worldWidth - 1)) != 0) {
      throw new IllegalArgumentException("worldWidth must be a power of 2");
    }
    if ((worldHeight & (worldHeight - 1)) != 0) {
      throw new IllegalArgumentException("worldHeight must be a power of 2");
    }
  }
}