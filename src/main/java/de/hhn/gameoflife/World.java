package de.hhn.gameoflife;

import java.awt.image.BufferedImage;
import java.util.BitSet;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class World {
  private final Drawable<BitSet> ui;
  private final TPS tps;
  private final int worldWidth;
  private final int worldHeight;
  private int minTickTime = 300;
  private final int worldSize;
  private final int worldHeightMinusOne;
  private final int worldWidthMinusOne;
  private final int logWorldWidth;
  private BitSet worldDataA;
  private BitSet worldDataB;
  private boolean paused = true;
  private final ScheduledExecutorService sheduler = Executors.newSingleThreadScheduledExecutor();
  private Runnable[] calcTickParts;
  private CompletableFuture<?>[] calcTickPartsFutures;
  private final Object lock = new Object();

  public World(
      final Settings settings, final Drawable<BitSet> ui, final Random rand, final TPS tps) {
    this.ui = ui;
    this.tps = tps;
    this.worldWidth = settings.worldWidth();
    this.worldHeight = settings.worldHeight();
    this.worldSize = this.worldWidth * this.worldHeight;
    this.worldHeightMinusOne = this.worldHeight - 1;
    this.worldWidthMinusOne = this.worldWidth - 1;
    this.logWorldWidth = Utils.log2(this.worldWidth);
    this.worldDataA = new BitSet(this.worldSize);
    this.worldDataB = new BitSet(this.worldSize);
    for (var i = 0; i < this.worldSize; ++i) {
      // randomly decide if the cell is alive or dead
      final var alive = rand.nextBoolean();
      this.worldDataA.set(i, alive);
      this.worldDataB.set(i, alive);
    }

    // how big is the world?
    if (this.worldSize >= 1_048_576) { // 1048576 = 1024 * 1024
      final var partsCount =
          Math.max(
              1, (int) Math.pow(2, (int) Utils.log2(Runtime.getRuntime().availableProcessors())));

      this.calcTickParts = new Runnable[partsCount];
      this.calcTickPartsFutures = new CompletableFuture<?>[partsCount];
      final var partSize = this.worldSize / partsCount;
      for (int i = 0; i < partsCount; ++i) {
        final var start = partSize * i;
        final var end = partSize * (i + 1);
        this.calcTickParts[i] =
            () -> {
              calcTick(start, end);
            };
      }
      // start the game loop
      this.sheduler.scheduleWithFixedDelay(this::tickAsync, 500, 1, TimeUnit.NANOSECONDS);
    } else {
      // start the game loop
      this.sheduler.scheduleWithFixedDelay(this::tickSync, 500, 1, TimeUnit.NANOSECONDS);
    }
  }

  /**
   * Sets the state of the cell at the given point to the given state.
   *
   * @param x the x coordinate of the point
   * @param y the y coordinate of the point
   * @param state the new state of the cell
   */
  public void togglePoint(final int x, final int y, final boolean state) {
    final var index = (y * this.worldWidth) + x;
    this.worldDataA.set(index, state);
  }

  /**
   * Toggles the state of the cell at the given point and returns its new state.
   *
   * @param x the x coordinate of the point
   * @param y the y coordinate of the point
   * @return the new state of the cell
   */
  public boolean togglePoint(final int x, final int y) {
    final var index = (y * this.worldWidth) + x;
    this.worldDataA.flip(index);
    return this.worldDataA.get(index);
  }

  /** calculate next generation */
  public void calcTick() {
    calcTick(0, this.worldSize);
  }

  /** calculate next generation */
  public void calcTick(final int start, final int end) {
    // initialize local variables
    int x = start & this.worldWidthMinusOne;
    int xPlusOne = x + 1;
    int xMinusOne = x - 1;
    int y = start >> this.logWorldWidth;
    int yPlusOne = y + 1;
    int yMinusOne = y - 1;
    int i = start;
    final int neighborsIndexes[] = new int[8];
    int livingNeighbors;
    int neighborIndex;
    boolean alive;

    // iterate over all cells
    while (y < worldHeight && i < end) {
      while (x < worldWidth) {
        // calculate the indexes of the neighbors for a torus world
        neighborsIndexes[0] =
            (((yMinusOne + this.worldHeight) & this.worldHeightMinusOne) << this.logWorldWidth)
                + ((xMinusOne + this.worldWidth) & this.worldWidthMinusOne);
        neighborsIndexes[1] =
            (((yMinusOne + this.worldHeight) & this.worldHeightMinusOne) << this.logWorldWidth) + x;
        neighborsIndexes[2] =
            (((yMinusOne + this.worldHeight) & this.worldHeightMinusOne) << this.logWorldWidth)
                + ((xPlusOne) & this.worldWidthMinusOne);
        neighborsIndexes[3] =
            (y << this.logWorldWidth) + ((xMinusOne + this.worldWidth) & this.worldWidthMinusOne);
        neighborsIndexes[4] = (y << this.logWorldWidth) + ((xPlusOne) & this.worldWidthMinusOne);
        neighborsIndexes[5] =
            (((yPlusOne) & this.worldHeightMinusOne) << this.logWorldWidth)
                + ((xMinusOne + this.worldWidth) & this.worldWidthMinusOne);
        neighborsIndexes[6] = (((yPlusOne) & this.worldHeightMinusOne) << this.logWorldWidth) + x;
        neighborsIndexes[7] =
            (((yPlusOne) & this.worldHeightMinusOne) << this.logWorldWidth)
                + ((xPlusOne) & this.worldWidthMinusOne);

        // count the living neighbors
        livingNeighbors = 0;
        alive = this.worldDataA.get(i);
        for (int j = 0; j < 8; ++j) {
          neighborIndex = neighborsIndexes[j];
          if (this.worldDataA.get(neighborIndex)) {
            ++livingNeighbors;
          }
        }

        // alive1 = alive0 ? (2 or 3 neighbors) : (3 neighbors)
        this.worldDataB.set(i++, livingNeighbors == 3 || alive && livingNeighbors == 2);
        xMinusOne = x;
        x = xPlusOne;
        xPlusOne = x + 1;
      }
      yMinusOne = y;
      y = yPlusOne;
      yPlusOne = y + 1;
      x = 0;
      xMinusOne = this.worldWidthMinusOne;
      xPlusOne = 1;
    }
  }

  public boolean togglePaused() {
    return this.paused = !this.paused;
  }

  /** clear the world */
  public void clear() {
    final var wasPaused = this.paused;
    this.paused = true;
    synchronized (this.lock) {
      this.worldDataA.clear();
      this.worldDataB.clear();
      this.ui.draw(this.worldDataA);
    }
    this.paused = wasPaused;
  }

  public void overwriteWorldData(final BitSet in) {
    final var wasPaused = this.paused;
    this.paused = true;
    synchronized (this.lock) {
      this.worldDataA.clear();
      this.worldDataA.or(in);
      this.ui.draw(this.worldDataA);
    }
    this.paused = wasPaused;
  }

  public void setMinTickTime(final int value) {
    this.minTickTime = value;
  }

  public int getMinTickTime() {
    return this.minTickTime;
  }

  public BitSet getWorldData() {
    return this.worldDataA;
  }

  public Boolean getPaused() {
    return this.paused;
  }

  public void setPaused(final boolean paused) {
    synchronized (this.lock) {
      this.paused = paused;
    }
  }

  public void dispose() {}

  public void setDataFrom(final BufferedImage resized) {
    final var wasPaused = this.paused;
    this.paused = true;
    for (var y = 0; y < this.worldHeight; ++y) {
      for (var x = 0; x < this.worldWidth; ++x) {
        final var pixel = resized.getRGB(x, y);
        final var redChannel = (pixel >> 16) & 0xFF;
        final var index = y * this.worldWidth + x;
        this.worldDataA.set(index, redChannel > 127);
      }
    }
    this.ui.draw(this.worldDataA);
    this.paused = wasPaused;
  }

  /** Trigger tick (next generation) synchronously */
  private void tickSync() {
    // check if the game is running
    if (this.paused) {
      return;
    }

    synchronized (this.lock) {
      // save start time
      final var start = System.nanoTime();

      // calculate next generation
      calcTick(0, this.worldSize);

      // calculate time spend for this tick
      final var tickTime = System.nanoTime() - start;
      this.tps.add(tickTime);

      // sleep if the tick was too fast
      final var sleepTime = this.minTickTime - tickTime / 1000000L;
      if (sleepTime > 0L) {
        try {
          Thread.sleep(sleepTime);
        } catch (final InterruptedException e) {
          // swap the world data a and b; a stays primary
          final var tmp = this.worldDataA;
          this.worldDataA = this.worldDataB;
          this.worldDataB = tmp;

          return;
        }
      }

      // swap the world data a and b; a stays primary
      final var tmp = this.worldDataA;
      this.worldDataA = this.worldDataB;
      this.worldDataB = tmp;

      // pass the new generation to the UI
      this.ui.draw(this.worldDataA);
    }
  }

  /** Trigger tick (next generation) asynchronously */
  private void tickAsync() {
    // check if the game is running
    if (this.paused) {
      return;
    }

    synchronized (this.lock) {
      // save start time
      final var start = System.nanoTime();

      // calculate next generation
      final var partsCount = this.calcTickParts.length;
      for (int i = 0; i < partsCount; ++i) {
        this.calcTickPartsFutures[i] = CompletableFuture.runAsync(this.calcTickParts[i]);
      }
      final CompletableFuture<Void> allDoneFuture = CompletableFuture.allOf(calcTickPartsFutures);
      try {
        allDoneFuture.get();
      } catch (final Exception e) {
        // ignore
        return;
      }

      // calculate time spend for this tick
      final var tickTime = System.nanoTime() - start;
      this.tps.add(tickTime);

      // sleep if the tick was too fast
      final var sleepTime = this.minTickTime - tickTime / 1000000L;
      if (sleepTime > 0L) {
        try {
          Thread.sleep(sleepTime);
        } catch (final InterruptedException e) {
          // swap the world data a and b; a stays primary
          final var tmp = this.worldDataA;
          this.worldDataA = this.worldDataB;
          this.worldDataB = tmp;

          return;
        }
      }

      // swap the world data a and b; a stays primary
      final var tmp = this.worldDataA;
      this.worldDataA = this.worldDataB;
      this.worldDataB = tmp;

      // pass the new generation to the UI
      this.ui.draw(this.worldDataA);
    }
  }

  public BitSet getWorldDataB() {
    return this.worldDataB;
  }
}