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
  private final TPS tps;
  private final int worldWidth;
  private final int worldHeight;
  private int minTickTime = 150;
  private final int worldSize;
  private final int worldHeightMinusOne;
  private final int worldWidthMinusOne;
  private final int logWorldWidth;
  private IntSet worldDataA;
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
      final TPS tps,
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
    this.worldDataA = new IntSet(this.worldSize);
    this.livingNeighbors = new IntMap(this.worldSize);
    for (var i = 0; i < this.worldSize; ++i) {
      // randomly decide if the cell is alive or dead
      final var alive = rand.nextBoolean();
      if (alive) {
        this.worldDataA.add(i);
      }
    }
    this.ui.draw(this.worldDataA);

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
    if (state) {
      this.worldDataA.add(index);
    } else {
      this.worldDataA.remove(index);
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
    if (this.worldDataA.contains(index)) {
      this.worldDataA.remove(index);
      return false;
    } else {
      this.worldDataA.add(index);
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
        if (this.worldDataA.contains(i)) {
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

  public boolean togglePaused() {
    return this.paused = !this.paused;
  }

  /** clear the world */
  public void clear() {
    final var wasPaused = this.paused;
    this.paused = true;
    try {
      this.worldDataSem.acquire();
      this.worldDataA.clear();
      this.livingNeighbors.clear();
      this.ui.draw(this.worldDataA);
    } catch (final InterruptedException e) {
      e.printStackTrace();
    } finally {
      this.worldDataSem.release();
    }
    this.paused = wasPaused;
  }

  public void overwriteWorldData(final IntSet in) {
    final var wasPaused = this.paused;
    this.paused = true;
    try {
      this.worldDataSem.acquire();
      this.worldDataA.clear();
      this.worldDataA.addAll(in);
      this.ui.draw(this.worldDataA);
    } catch (final InterruptedException e) {
      e.printStackTrace();
    } finally {
      this.worldDataSem.release();
    }
    this.paused = wasPaused;
  }

  public void setMinTickTime(final int value) {
    this.minTickTime = value;
  }

  public int getMinTickTime() {
    return this.minTickTime;
  }

  public IntSet getWorldData() {
    return this.worldDataA;
  }

  public Boolean getPaused() {
    return this.paused;
  }

  public void setPaused(final boolean paused) {
    this.paused = paused;
  }

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
      this.worldDataA = null;
      this.livingNeighbors.clear();
    }
  }

  public void setDataFrom(final BufferedImage resized) {
    final var wasPaused = this.paused;
    this.paused = true;
    for (var y = 0; y < this.worldHeight; ++y) {
      for (var x = 0; x < this.worldWidth; ++x) {
        final var pixel = resized.getRGB(x, y);
        final var redChannel = (pixel >> 16) & 0xFF;
        final var index = y * this.worldWidth + x;
        if (redChannel > 127) {
          this.worldDataA.add(index);
        }
      }
    }
    this.ui.draw(this.worldDataA);
    this.paused = wasPaused;
  }

  public IntSet getWorldDataA() {
    return this.worldDataA;
  }

  private void applyLivingNeighborCount() {
    boolean alive;
    int count;
    for (var i = 0; i < this.worldSize; ++i) {
      if (this.livingNeighbors.containsKey(i)) {
        count = this.livingNeighbors.get(i);
        alive = this.worldDataA.contains(i);
        if (alive) {
          if (count < 2 || count > 3) {
            this.worldDataA.remove(i);
          }
        } else {
          if (count == 3) {
            this.worldDataA.add(i);
          }
        }
      } else {
        this.worldDataA.remove(i);
      }
    }
    this.livingNeighbors.clear();
  }

  /** Trigger tick (next generation) synchronously */
  private void tickSync() {
    // check if the game is running
    if (this.paused) {
      return;
    }

    try {
      this.worldDataSem.acquire();
      // save start time
      final var start = System.nanoTime();

      // calculate next generation
      this.calcTick(0, this.worldSize);
      this.applyLivingNeighborCount();

      // calculate time spend for this tick
      final var tickTime = System.nanoTime() - start;
      this.tps.add(tickTime);

      // sleep if the tick was too fast
      final var sleepTime = this.minTickTime - tickTime / 1000000L;
      if (sleepTime > 0L) {
        Thread.sleep(sleepTime);
      }
    } catch (final InterruptedException e) {
      e.printStackTrace();
      return;
    } finally {
      this.worldDataSem.release();
    }

    // pass the new generation to the UI
    this.ui.draw(this.worldDataA);
  }

  /** Trigger tick (next generation) asynchronously */
  private void tickAsync() {
    // check if the game is running
    if (this.paused) {
      return;
    }

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
      final var tickTime = System.nanoTime() - start;
      this.tps.add(tickTime);

      // sleep if the tick was too fast
      final var sleepTime = this.minTickTime - tickTime / 1000000L;
      if (sleepTime > 0L) {
        Thread.sleep(sleepTime);
      }
    } catch (final InterruptedException e) {
      // ignore
      return;
    } finally {
      this.worldDataSem.release();
    }

    // pass the new generation to the UI
    this.ui.draw(this.worldDataA);
  }
}