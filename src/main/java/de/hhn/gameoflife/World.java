package de.hhn.gameoflife;

import java.awt.image.BufferedImage;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class World {
  private final Drawable<IntSet> ui;
  private final FPS tps;
  private final int worldWidth;
  private final int worldHeight;
  private int minTickTime = 50;
  private final int worldSize;
  private final int worldHeightMinusOne;
  private final int worldWidthMinusOne;
  private final int logWorldWidth;
  private IntSet worldData;
  private final IntMap livingNeighbors;
  private boolean paused = true;
  private final ScheduledExecutorService sheduler = Executors.newSingleThreadScheduledExecutor();
  private Runnable[] calcTickParts;
  private CompletableFuture<?>[] calcTickPartsFutures;
  private final Semaphore worldDataSem;
  private boolean disposed;

  public World(
      final Settings settings,
      final Drawable<IntSet> ui,
      final Random rand,
      final FPS tps,
      final Semaphore worldDataSem) {
    this.ui = ui;
    this.tps = tps;
    this.worldDataSem = worldDataSem;
    this.worldWidth = settings.worldWidth();
    this.worldHeight = settings.worldHeight();
    this.worldSize = this.worldWidth * this.worldHeight;
    this.worldHeightMinusOne = this.worldHeight - 1;
    this.worldWidthMinusOne = this.worldWidth - 1;
    this.logWorldWidth = Utils.log2(this.worldWidth);
    this.worldData = new IntSet(this.worldSize);
    this.livingNeighbors = new IntMap(this.worldSize);
    for (var i = 0; i < this.worldSize; ++i) {
      // randomly decide if the cell is alive or dead
      final var alive = rand.nextBoolean();
      if (alive) {
        this.worldData.add(i);
      }
    }
    this.ui.set(this.worldData);
    this.ui.draw();

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
    this.ui.set(index, state);
    if (state) {
      this.worldData.add(index);
    } else {
      this.worldData.remove(index);
    }
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
    if (this.worldData.contains(index)) {
      this.worldData.remove(index);
      this.ui.set(index, false);
      return false;
    } else {
      this.worldData.add(index);
      this.ui.set(index, true);
      return true;
    }
  }

  /** calculate next generation */
  public void calcTick() {
    this.calcTick(0, this.worldSize);
  }

  /** calculate next generation */
  public void calcTick(final int start, final int end) {
    // initialize local variables
    int x = start & this.worldWidthMinusOne; // start % this.worldWidth
    int xPlusOne = x + 1;
    int xMinusOne = x - 1;
    int y = start >> this.logWorldWidth; // start / this.worldWidth
    int yPlusOne = y + 1;
    int yMinusOne = y - 1;
    int i = start;

    // iterate over all cells
    while (y < worldHeight && i < end) {
      while (x < worldWidth) {
        if (this.worldData.contains(i)) {
          // calculate the indexes of the neighbors for a torus world
          this.livingNeighbors.increment(
              (((yMinusOne + this.worldHeight) & this.worldHeightMinusOne) << this.logWorldWidth)
                  + ((xMinusOne + this.worldWidth) & this.worldWidthMinusOne));
          this.livingNeighbors.increment(
              (((yMinusOne + this.worldHeight) & this.worldHeightMinusOne) << this.logWorldWidth)
                  + x);
          this.livingNeighbors.increment(
              (((yMinusOne + this.worldHeight) & this.worldHeightMinusOne) << this.logWorldWidth)
                  + ((xPlusOne) & this.worldWidthMinusOne));
          this.livingNeighbors.increment(
              (y << this.logWorldWidth)
                  + ((xMinusOne + this.worldWidth) & this.worldWidthMinusOne));
          this.livingNeighbors.increment(
              (y << this.logWorldWidth) + ((xPlusOne) & this.worldWidthMinusOne));
          this.livingNeighbors.increment(
              (((yPlusOne) & this.worldHeightMinusOne) << this.logWorldWidth)
                  + ((xMinusOne + this.worldWidth) & this.worldWidthMinusOne));
          this.livingNeighbors.increment(
              (((yPlusOne) & this.worldHeightMinusOne) << this.logWorldWidth) + x);
          this.livingNeighbors.increment(
              (((yPlusOne) & this.worldHeightMinusOne) << this.logWorldWidth)
                  + ((xPlusOne) & this.worldWidthMinusOne));
        }
        xMinusOne = x;
        x = xPlusOne;
        xPlusOne = x + 1;
        ++i;
      }
      yMinusOne = y;
      y = yPlusOne;
      yPlusOne = y + 1;
      x = 0;
      xMinusOne = this.worldWidthMinusOne;
      xPlusOne = 1;
    }
  }

  /** clear the world */
  public void clear() {
    final var wasPaused = this.paused;
    this.paused = true;
    try {
      this.worldDataSem.acquire();
      this.worldData.clear();
      this.livingNeighbors.clear();
      this.ui.set(this.worldData);
      this.ui.draw();
    } catch (final InterruptedException e) {
      e.printStackTrace();
    } finally {
      this.worldDataSem.release();
    }
    this.paused = wasPaused;
  }

  /** overwrite the world data with the given data */
  public void overwriteWorldData(final IntSet in) {
    final var wasPaused = this.paused;
    this.paused = true;
    try {
      this.worldDataSem.acquire();
      this.worldData.overwrite(in);
      this.ui.set(this.worldData);
      this.ui.draw();
    } catch (final InterruptedException e) {
      e.printStackTrace();
    } finally {
      this.worldDataSem.release();
    }
    this.paused = wasPaused;
  }

  /** Set the minimum time for one frame */
  public void setMinTickTime(final int value) {
    this.minTickTime = value;
  }

  /** Get the minimum time for one frame */
  public int getMinTickTime() {
    return this.minTickTime;
  }

  /** Get the games paused state */
  public Boolean getPaused() {
    return this.paused;
  }

  /** Set games paused state to the given value */
  public void setPaused(final boolean paused) {
    this.paused = paused;
  }

  /** Toggle the games paused state */
  public boolean togglePaused() {
    return this.paused = !this.paused;
  }

  /** clean up */
  public void dispose() {
    if (this.disposed) {
      return;
    }
    this.disposed = true;
    this.sheduler.shutdownNow();
    try {
      this.sheduler.awaitTermination(5, TimeUnit.SECONDS);
    } catch (final InterruptedException e) {
      System.err.println("Interrupted while waiting for sheduler to terminate");
    } finally {
      this.worldData = null;
      this.livingNeighbors.clear();
    }
  }

  /** Import world data from a BufferedImage */
  public void setDataFrom(final BufferedImage resized) {
    final var wasPaused = this.paused;
    this.paused = true;
    for (var y = 0; y < this.worldHeight; ++y) {
      for (var x = 0; x < this.worldWidth; ++x) {
        final var pixel = resized.getRGB(x, y);
        final var redChannel = (pixel >> 16) & 0xFF;
        final var index = y * this.worldWidth + x;
        if (redChannel > 127) {
          this.worldData.add(index);
        }
      }
    }
    this.ui.set(this.worldData);
    this.ui.draw();
    this.paused = wasPaused;
  }

  public IntSet getWorldData() {
    return this.worldData;
  }

  /** Use collected living neighbor count to apply the rules of the game */
  private void applyLivingNeighborCount() {
    boolean alive;
    int count;
    for (var i = 0; i < this.worldSize; ++i) {
      if (this.livingNeighbors.containsKey(i)) {
        count = this.livingNeighbors.get(i);
        alive = this.worldData.contains(i);
        if (alive) {
          if (count < 2 || count > 3) {
            this.worldData.remove(i);
            this.ui.set(i, false);
          }
        } else {
          if (count == 3) {
            this.worldData.add(i);
            this.ui.set(i, true);
          }
        }
      } else {
        this.worldData.remove(i);
        this.ui.set(i, false);
      }
    }
    this.ui.draw();
    this.livingNeighbors.clear();
  }

  /** Trigger tick (next generation) synchronously */
  private void tickSync() {
    // check if the game is running
    if (this.paused) {
      return;
    }

    long tickTime;

    try {
      this.worldDataSem.acquire();
      // save start time
      final var start = System.nanoTime();

      // calculate next generation
      this.calcTick(0, this.worldSize);
      this.applyLivingNeighborCount();

      // calculate time spend for this tick
      tickTime = System.nanoTime() - start;
      this.tps.add(tickTime);

    } catch (final InterruptedException e) {
      return;
    } finally {
      this.worldDataSem.release();
    }
    // sleep if the tick was too fast
    final var sleepTime = this.minTickTime - tickTime / 1000000L;
    if (sleepTime > 0L) {
      try {
        Thread.sleep(sleepTime);
      } catch (final InterruptedException e) {
        return;
      }
    }
  }

  /** Trigger tick (next generation) asynchronously */
  private void tickAsync() {
    // check if the game is running
    if (this.paused) {
      return;
    }

    long tickTime;

    try {
      this.worldDataSem.acquire();

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
        this.applyLivingNeighborCount();
      } catch (final Exception e) {
        // ignore
        return;
      }

      // calculate time spend for this tick
      tickTime = System.nanoTime() - start;
      this.tps.add(tickTime);
    } catch (final InterruptedException e) {
      // ignore
      return;
    } finally {
      this.worldDataSem.release();
    }

    // sleep if the tick was too fast
    final var sleepTime = this.minTickTime - tickTime / 1000000L;
    if (sleepTime > 0L) {
      try {
        Thread.sleep(sleepTime);
      } catch (final InterruptedException e) {
        // ignore
        return;
      }
    }
  }
}