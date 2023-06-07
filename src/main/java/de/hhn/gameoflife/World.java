package de.hhn.gameoflife;

import java.awt.image.BufferedImage;
import java.util.BitSet;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class World {
  private final Drawable<BitSet> ui;
  private final TPS tps;
  private final int worldWidth;
  private final int worldHeight;
  private int minTickTime = 150;
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
  private final Semaphore worldDataSem;
  private boolean disposed;

  public World(
      final Settings settings,
      final Drawable<BitSet> ui,
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
    this.worldDataA = new BitSet(this.worldSize);
    this.worldDataB = new BitSet(this.worldSize);
    for (var i = 0; i < this.worldSize; ++i) {
      // randomly decide if the cell is alive or dead
      final var alive = rand.nextBoolean();
      this.worldDataA.set(i, alive);
      this.worldDataB.set(i, alive);
    }
    this.ui.draw(this.worldDataA);

    // start the game loop
    this.sheduler.scheduleWithFixedDelay(this::tickSync, 500, 1, TimeUnit.NANOSECONDS);
  }

  /**
   * Sets the state of the cell at the given point to the given state.
   *
   * @param x     the x coordinate of the point
   * @param y     the y coordinate of the point
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
    final var livingCells = new int[6 * 6];
    final var neighbors = new int[6 * 6];
    int chunkWorldY;
    int chunkWorldX;
    int chunkY;
    int chunkYMinusOne;
    int chunkX;
    int chunkXMinusOne;
    int livingCellsCount;

    // divide world into 4x4 chunks and iterate over them
    for (chunkWorldY = 0; chunkWorldY < (this.worldHeight >> 2); ++chunkWorldY) {
      for (chunkWorldX = 0; chunkWorldX < (this.worldWidth >> 2); ++chunkWorldX) {
        // save all living cells in this chunk (including the border
        // cells)
        livingCellsCount = 0;
        chunkY = 0;
        chunkYMinusOne = -1;
        while (chunkY != 6) {
          final var worldY = ((chunkWorldY << 2) + chunkYMinusOne + this.worldHeight) & this.worldHeightMinusOne;
          chunkX = 0;
          chunkXMinusOne = -1;
          while (chunkX != 6) {
            final var worldX = ((chunkWorldX << 2) + chunkXMinusOne + this.worldWidth) & this.worldWidthMinusOne;
            final var chunkIndex = ((chunkY) * 6) + chunkX;
            final var worldIndex = (worldY << this.logWorldWidth) + worldX;
            if (this.worldDataA.get(worldIndex)) {
              livingCells[livingCellsCount] = chunkIndex;
              ++livingCellsCount;
            }
            chunkXMinusOne = chunkX;
            ++chunkX;
          }
          chunkYMinusOne = chunkY;
          ++chunkY;
        }

        // count neighbors
        var neighborIndex = 0;
        for (var i = 0; i < 36; ++i) {
          neighbors[i] = 0;
        }
        for (var i = 0; i < livingCellsCount; ++i) {
          final var livingIndex = livingCells[i];
          if ((neighborIndex = (livingIndex + 1)) < 36) {
            ++neighbors[neighborIndex];
          }
          if ((neighborIndex = (livingIndex + 5)) < 36) {
            ++neighbors[neighborIndex];
          }
          if ((neighborIndex = (livingIndex + 6)) < 36) {
            ++neighbors[neighborIndex];
          }
          if ((neighborIndex = (livingIndex + 7)) < 36) {
            ++neighbors[neighborIndex];
          }
          if ((neighborIndex = (livingIndex - 1)) >= 0) {
            ++neighbors[neighborIndex];
          }
          if ((neighborIndex = (livingIndex - 5)) >= 0) {
            ++neighbors[neighborIndex];
          }
          if ((neighborIndex = (livingIndex - 6)) >= 0) {
            ++neighbors[neighborIndex];
          }
          if ((neighborIndex = (livingIndex - 7)) >= 0) {
            ++neighbors[neighborIndex];
          }
        }

        // write new data to worldDataB
        chunkY = 1;
        chunkYMinusOne = 0;
        while (chunkY != 5) {
          chunkX = 1;
          chunkXMinusOne = 0;
          while (chunkX != 5) {
            final var chunkIndex = (chunkY * 6) + chunkX;
            final var worldY = (chunkWorldY << 2) + chunkYMinusOne;
            final var worldX = (chunkWorldX << 2) + chunkXMinusOne;
            final var worldIndex = (worldY << this.logWorldWidth) + worldX;
            this.worldDataB.set(
                worldIndex,
                neighbors[chunkIndex] == 3
                    || this.worldDataA.get(worldIndex) && neighbors[chunkIndex] == 2);
            chunkXMinusOne = chunkX;
            ++chunkX;
          }
          chunkYMinusOne = chunkY;
          ++chunkY;
        }
      }
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
      this.worldDataB.clear();
      this.ui.draw(this.worldDataA);
    } catch (final InterruptedException e) {
      e.printStackTrace();
    } finally {
      this.worldDataSem.release();
    }
    this.paused = wasPaused;
  }

  public void overwriteWorldData(final BitSet in) {
    final var wasPaused = this.paused;
    this.paused = true;
    try {
      this.worldDataSem.acquire();
      this.worldDataA.clear();
      this.worldDataA.or(in);
      this.worldDataB.clear();
      this.worldDataB.or(in);
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

  public BitSet getWorldData() {
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
      this.worldDataB = null;
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
        this.worldDataA.set(index, redChannel > 127);
      }
    }
    this.ui.draw(this.worldDataA);
    this.paused = wasPaused;
  }

  public final BitSet getWorldDataB() {
    return this.worldDataB;
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
      this.calcTick();

      // calculate time spend for this tick
      final var tickTime = System.nanoTime() - start;
      this.tps.add(tickTime);

      // sleep if the tick was too fast
      final var sleepTime = this.minTickTime - tickTime / 1000000L;
      if (sleepTime > 0L) {
        try {
          Thread.sleep(sleepTime);
        } catch (final InterruptedException e) {
          return;
        }
      }

      // swap the world data a and b; a stays primary
      final var tmp = this.worldDataA;
      this.worldDataA = this.worldDataB;
      this.worldDataB = tmp;
    } catch (final InterruptedException e) {
      return;
    } finally {
      this.worldDataSem.release();
    }

    // pass the new generation to the UI
    this.ui.draw(this.worldDataA);
  }
}