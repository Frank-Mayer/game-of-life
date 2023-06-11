package de.hhn.gameoflife.logic;

/** Shape drawing styles. */
public enum DrawingStyle {

  // Still Lives
  BLOCK(
      "Block",
      DrawingStyleCategory.STILL_LIFES,
      new Boolean[][] {
        {false, false, false, false},
        {false, true, true, false},
        {false, true, true, false},
        {false, false, false, false}
      }),

  BEEHIVE(
      "Beehive",
      DrawingStyleCategory.STILL_LIFES,
      new Boolean[][] {
        {false, false, false, false, false, false},
        {false, false, true, true, false, false},
        {false, true, false, false, true, false},
        {false, false, true, true, false, false},
        {false, false, false, false, false, false}
      }),

  LOAF(
      "Loaf",
      DrawingStyleCategory.STILL_LIFES,
      new Boolean[][] {
        {false, false, false, false, false, false},
        {false, false, true, true, false, false},
        {false, true, false, false, true, false},
        {false, false, true, false, true, false},
        {false, false, false, true, false, false},
        {false, false, false, false, false, false}
      }),

  BOAT(
      "Boat",
      DrawingStyleCategory.STILL_LIFES,
      new Boolean[][] {
        {false, false, false, false, false},
        {false, true, true, false, false},
        {false, true, false, true, false},
        {false, false, true, false, false},
        {false, false, false, false, false}
      }),

  TUB(
      "Tub",
      DrawingStyleCategory.STILL_LIFES,
      new Boolean[][] {
        {false, false, false, false, false},
        {false, false, true, false, false},
        {false, true, false, true, false},
        {false, false, true, false, false},
        {false, false, false, false, false}
      }),

  // Oscillators

  BLINKER(
      "Blinker",
      DrawingStyleCategory.OSCILLATORS,
      new Boolean[][] {
        {false, false, false, false, false},
        {false, false, false, false, false},
        {false, true, true, true, false},
        {false, false, false, false, false},
        {false, false, false, false, false}
      }),

  TOAD(
      "Toad",
      DrawingStyleCategory.OSCILLATORS,
      new Boolean[][] {
        {false, false, false, false, false, false},
        {false, false, false, true, false, false},
        {false, true, false, false, true, false},
        {false, true, false, false, true, false},
        {false, false, true, false, false, false},
        {false, false, false, false, false, false}
      }),

  BEACON(
      "Beacon",
      DrawingStyleCategory.OSCILLATORS,
      new Boolean[][] {
        {false, false, false, false, false, false},
        {false, true, true, false, false, false},
        {false, true, true, false, false, false},
        {false, false, false, true, true, false},
        {false, false, false, true, true, false},
        {false, false, false, false, false, false}
      }),

  PULSAR(
      "Pulsar",
      DrawingStyleCategory.OSCILLATORS,
      new Boolean[][] {
        {
          false, false, false, false, false, false, false, false, false, false, false, false, false,
          false, false, false, false
        },
        {
          false, false, false, false, false, false, false, false, false, false, false, false, false,
          false, false, false, false
        },
        {
          false, false, false, false, true, true, true, false, false, false, true, true, true,
          false, false, false, false
        },
        {
          false, false, false, false, false, false, false, false, false, false, false, false, false,
          false, false, false, false
        },
        {
          false, false, true, false, false, false, false, true, false, true, false, false, false,
          false, true, false, false
        },
        {
          false, false, true, false, false, false, false, true, false, true, false, false, false,
          false, true, false, false
        },
        {
          false, false, true, false, false, false, false, true, false, true, false, false, false,
          false, true, false, false
        },
        {
          false, false, false, false, true, true, true, false, false, false, true, true, true,
          false, false, false, false
        },
        {
          false, false, false, false, false, false, false, false, false, false, false, false, false,
          false, false, false, false
        },
        {
          false, false, false, false, true, true, true, false, false, false, true, true, true,
          false, false, false, false
        },
        {
          false, false, true, false, false, false, false, true, false, true, false, false, false,
          false, true, false, false
        },
        {
          false, false, true, false, false, false, false, true, false, true, false, false, false,
          false, true, false, false
        },
        {
          false, false, true, false, false, false, false, true, false, true, false, false, false,
          false, true, false, false
        },
        {
          false, false, false, false, false, false, false, false, false, false, false, false, false,
          false, false, false, false
        },
        {
          false, false, false, false, true, true, true, false, false, false, true, true, true,
          false, false, false, false
        },
        {
          false, false, false, false, false, false, false, false, false, false, false, false, false,
          false, false, false, false
        },
        {
          false, false, false, false, false, false, false, false, false, false, false, false, false,
          false, false, false, false
        },
      }),

  PENTADECATHLON(
      "Pentadecathlon",
      DrawingStyleCategory.OSCILLATORS,
      new Boolean[][] {
        {false, false, false, false, false, false, false, false, false, false, false},
        {false, false, false, false, false, false, false, false, false, false, false},
        {false, false, false, false, false, false, false, false, false, false, false},
        {false, false, false, false, false, false, false, false, false, false, false},
        {false, false, false, false, false, true, false, false, false, false, false},
        {false, false, false, false, false, true, false, false, false, false, false},
        {false, false, false, false, true, false, true, false, false, false, false},
        {false, false, false, false, false, true, false, false, false, false, false},
        {false, false, false, false, false, true, false, false, false, false, false},
        {false, false, false, false, false, true, false, false, false, false, false},
        {false, false, false, false, false, true, false, false, false, false, false},
        {false, false, false, false, true, false, true, false, false, false, false},
        {false, false, false, false, false, true, false, false, false, false, false},
        {false, false, false, false, false, true, false, false, false, false, false},
        {false, false, false, false, false, false, false, false, false, false, false},
        {false, false, false, false, false, false, false, false, false, false, false},
        {false, false, false, false, false, false, false, false, false, false, false},
        {false, false, false, false, false, false, false, false, false, false, false}
      }),

  // Spaceships

  GLIDER(
      "Glider",
      DrawingStyleCategory.SPACESHIPS,
      new Boolean[][] {
        {false, false, false, false, false},
        {false, true, false, false, false},
        {false, false, true, true, false},
        {false, true, true, false, false},
        {false, false, false, false, false}
      }),

  // Light-Weighted-Spaceship
  LWSS(
      "LWSS",
      DrawingStyleCategory.SPACESHIPS,
      new Boolean[][] {
        {false, false, false, false, false, false, false},
        {false, true, false, false, true, false, false},
        {false, false, false, false, false, true, false},
        {false, true, false, false, false, true, false},
        {false, false, true, true, true, true, false},
        {false, false, false, false, false, false, false},
      }),

  // Middle-Weighted-Spaceship
  MWSS(
      "MWSS",
      DrawingStyleCategory.SPACESHIPS,
      new Boolean[][] {
        {false, false, false, false, false, false, false, false},
        {false, false, false, false, true, true, false, false},
        {false, true, true, true, false, true, true, false},
        {false, true, true, true, true, true, false, false},
        {false, false, true, true, true, false, false, false},
        {false, false, false, false, false, false, false, false},
      }),

  // High-Weighted-Spaceship
  HWSS(
      "HWSS",
      DrawingStyleCategory.SPACESHIPS,
      new Boolean[][] {
        {false, false, false, false, false, false, false, false, false},
        {false, false, false, false, false, true, true, false, false},
        {false, true, true, true, true, false, true, true, false},
        {false, true, true, true, true, true, true, false, false},
        {false, false, true, true, true, true, false, false, false},
        {false, false, false, false, false, false, false, false, false},
      });

  private final Boolean[][] structure;
  private final String name;

  private final DrawingStyleCategory category;

  DrawingStyle(
      final String name, final DrawingStyleCategory category, final Boolean[][] structure) {
    this.structure = structure;
    this.name = name;
    this.category = category;
  }

  public Boolean[][] getStructure() {
    return this.structure;
  }

  public String getName() {
    return this.name;
  }

  public DrawingStyleCategory getCategory() {
    return this.category;
  }
}