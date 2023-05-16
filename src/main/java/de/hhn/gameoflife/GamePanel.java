package de.hhn.gameoflife;

import static de.hhn.gameoflife.State.useState;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;

/**
 * The main game panel.
 *
 * <p>This class encapsulates a Game of Life. Indipendent of the Window management.
 */
public class GamePanel extends JPanel {

  private static int log2(final Number x) {
    return (int) Math.ceil(Math.log(x.doubleValue()) / Math.log(2));
  }

  private final int worldWidth;
  private final int worldHeight;
  private final int worldSize;
  private final int worldHeightMinusOne;
  private final int worldWidthMinusOne;
  private final int logWorldWidth;
  private final WorldUI worldUI;

  private final TPS tpsLabel;

  /** the executor service to schedule the ticks */
  private final ScheduledExecutorService sheduler;

  // world data
  /** primary world data */
  private BitSet worldDataA;

  /** secondary world data; temporary storage for the next generation */
  private BitSet worldDataB;

  private boolean paused = true;

  /** the minimum time for one tick in ms */
  private long minTickTime = 0;

  /**
   * the tick parts to be executed in parallel.
   *
   * <p>Only used for the parallel implementation (for big worlds).
   */
  private Runnable[] calcTickParts;

  /** lock object to synchronize write access to the world data */
  private final Object lock = new Object();

  private boolean disposed = false;

  public GamePanel(final int width, final int height) {

    // initialize ui
    this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    // tick time label
    this.tpsLabel = new TPS();
    this.add(this.tpsLabel);
    // min tick time slider (delay)
    final var minTickTimeLabel = new JLabel("Min Tick Time (0ms)");
    this.add(minTickTimeLabel);
    final var minTickTimeSlider = new JSlider(0, 1000, 0);
    minTickTimeSlider.setPaintLabels(true);
    minTickTimeSlider.setPaintTrack(true);
    minTickTimeSlider.setPaintTicks(true);
    minTickTimeSlider.setMajorTickSpacing(100);
    minTickTimeSlider.setMinorTickSpacing(10);
    minTickTimeSlider.setSnapToTicks(true);
    minTickTimeSlider.addChangeListener(
        e -> {
          this.minTickTime = minTickTimeSlider.getValue();
          minTickTimeLabel.setText(String.format("Min Tick Time (%d ms)", this.minTickTime));
        });
    minTickTimeSlider.setMajorTickSpacing(100);
    minTickTimeSlider.setMinorTickSpacing(10);
    minTickTimeSlider.setPaintTicks(true);
    this.add(minTickTimeSlider);

    // initialize the "world"
    this.worldWidth = width;
    this.worldHeight = height;
    this.worldSize = width * height;
    this.worldHeightMinusOne = this.worldHeight - 1;
    this.worldWidthMinusOne = this.worldWidth - 1;
    this.logWorldWidth = GamePanel.log2(this.worldWidth);
    this.worldDataA = new BitSet(this.worldSize);
    this.worldDataB = new BitSet(this.worldSize);
    final var rand = new Random();
    for (int i = 0; i < this.worldSize; ++i) {
      // randomly decide if the cell is alive or dead
      final var alive = rand.nextBoolean();
      this.worldDataA.set(i, alive);
      this.worldDataB.set(i, alive);
    }
    // world ui to display the world
    this.worldUI = new WorldUI(this.worldDataA, width, height);
    this.add(this.worldUI);
    // add mouse listener to toggle cells
    final var drawNewState = useState(false);
    final var wasPaused = useState(false);
    final var relativeBoundingRect = new Rectangle(0, 0, 0, 0);
    this.worldUI.addMouseListener(
        new MouseListener() {
          @Override
          public void mouseClicked(final MouseEvent e) {}

          @Override
          public void mousePressed(final MouseEvent e) {
            wasPaused.set(GamePanel.this.paused);
            GamePanel.this.paused = true;
            relativeBoundingRect.setSize(
                GamePanel.this.worldUI.getWidth(), GamePanel.this.worldUI.getHeight());

            synchronized (GamePanel.this.lock) {
              drawNewState.set(GamePanel.this.togglePoint(e.getPoint()));
              GamePanel.this.worldUI.draw(GamePanel.this.worldDataA);
            }
          }

          @Override
          public void mouseReleased(final MouseEvent e) {
            GamePanel.this.paused = wasPaused.get();
          }

          @Override
          public void mouseEntered(final MouseEvent e) {}

          @Override
          public void mouseExited(final MouseEvent e) {}
        });
    this.worldUI.addMouseMotionListener(
        new MouseMotionListener() {
          @Override
          public void mouseDragged(final MouseEvent e) {
            // if point is not in the worldUI, do nothing
            if (!relativeBoundingRect.contains(e.getPoint())) {
              return;
            }

            synchronized (GamePanel.this.lock) {
              GamePanel.this.togglePoint(e.getPoint(), drawNewState.get());
              GamePanel.this.worldUI.draw(GamePanel.this.worldDataA);
            }
          }

          @Override
          public void mouseMoved(final MouseEvent e) {}
        });

    this.sheduler = Executors.newSingleThreadScheduledExecutor();

    // how big is the world?
    if (this.worldSize >= 1_048_576) { // 1048576 = 1024 * 1024
      final var partsCount =
          Math.max(
              1,
              (int) Math.pow(2, (int) GamePanel.log2(Runtime.getRuntime().availableProcessors())));

      this.calcTickParts = new Runnable[partsCount];
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
   * @param point the point to toggle
   * @param state the new state of the cell
   */
  public void togglePoint(final Point point, final boolean state) {
    final var cellWidth = (double) GamePanel.this.worldUI.getWidth() / (double) this.worldWidth;
    final var cellHeight = (double) GamePanel.this.worldUI.getHeight() / (double) this.worldHeight;
    final var x = (int) (point.x / cellWidth);
    final var y = (int) (point.y / cellHeight);
    final var index = (y * this.worldWidth) + x;
    this.worldDataA.set(index, state);
  }

  /**
   * Toggles the state of the cell at the given point and returns its new state.
   *
   * @param point the point to toggle
   * @return the new state of the cell
   */
  public boolean togglePoint(final Point point) {
    final var cellWidth = (double) GamePanel.this.worldUI.getWidth() / (double) this.worldWidth;
    final var cellHeight = (double) GamePanel.this.worldUI.getHeight() / (double) this.worldHeight;
    final var x = (int) (point.x / cellWidth);
    final var y = (int) (point.y / cellHeight);
    final var index = (y * this.worldWidth) + x;
    this.worldDataA.flip(index);
    return this.worldDataA.get(index);
  }

  /** free resources */
  public void dispose() {
    synchronized (this.lock) {
      if (this.disposed) {
        return;
      }
      this.disposed = true;
      this.paused = true;
      this.sheduler.shutdownNow();
      this.worldDataA = null;
      this.worldDataB = null;
      this.worldUI.dispose();
    }
  }

  public Color getAliveColor() {
    if (this.worldUI == null) {
      return null;
    }
    return this.worldUI.getAliveColor();
  }

  public void setAliveColor(final Color color) {
    if (this.worldUI == null) {
      return;
    }
    this.worldUI.setAliveColor(color);
  }

  public Color getDeadColor() {
    if (this.worldUI == null) {
      return null;
    }
    return this.worldUI.getDeadColor();
  }

  public void setDeadColor(final Color color) {
    if (this.worldUI == null) {
      return;
    }
    this.worldUI.setDeadColor(color);
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
        GamePanel.this.worldDataB.set(i++, livingNeighbors == 3 || alive && livingNeighbors == 2);
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
    this.worldDataA.clear();
    this.worldDataB.clear();
    this.worldUI.draw(this.worldDataA);
  }

  public void overwriteWorldData(final BitSet in) {
    this.worldDataA.clear();
    this.worldDataA.or(in);
    this.worldUI.draw(this.worldDataA);
  }

  public BitSet getWorldDataB() {
    return this.worldDataB;
  }

  /** load world data from an image file */
  public void load(final File imageFile) {
    final var wasPaused = this.paused;
    this.paused = true;
    synchronized (this.lock) {
      BufferedImage img;
      try {
        img = ImageIO.read(imageFile);
        if (img == null) {
          Alert.show("Error", "The selected file is not a valid image file.", this.worldUI);
          this.paused = wasPaused;
          return;
        }
      } catch (final Exception e) {
        Alert.show("Error", e.getMessage(), this.worldUI);
        this.paused = wasPaused;
        return;
      }
      final BufferedImage resized =
          new BufferedImage(this.worldWidth, this.worldHeight, BufferedImage.TYPE_INT_RGB);
      final var g = resized.createGraphics();
      g.drawImage(img, 0, 0, this.worldWidth, this.worldHeight, null);
      // apply filters
      Dithering.grayScale(resized);
      Dithering.floydSteinberg(resized);
      // write pixels into world data
      for (int y = 0; y < this.worldHeight; ++y) {
        for (int x = 0; x < this.worldWidth; ++x) {
          final var pixel = resized.getRGB(x, y);
          final var redChannel = (pixel >> 16) & 0xFF;
          final var index = y * this.worldWidth + x;
          this.worldDataA.set(index, redChannel > 127);
        }
      }
      this.worldUI.draw(this.worldDataA);
      g.dispose();
    }
    this.paused = wasPaused;
  }

  /** save world data to an image file */
  public void save(final File imageFile) {
    final var wasPaused = this.paused;
    this.paused = true;
    synchronized (this.lock) {
      final var img = this.worldUI.getImage();
      final var fileName = imageFile.getName();
      final var dotIndex = fileName.lastIndexOf('.');
      final var ext =
          dotIndex == -1 || dotIndex == fileName.length() - 1
              ? "jpeg"
              : fileName.substring(dotIndex + 1);
      try {
        ImageIO.write(img, ext, imageFile);
      } catch (final Exception e) {
        Alert.show("Error", e.getMessage(), this.worldUI);
      }
    }
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
      this.tpsLabel.add(tickTime);

      // sleep if the tick was too fast
      final var sleepTime = this.minTickTime - tickTime / 1000000L;
      if (sleepTime > 0L) {
        try {
          Thread.sleep(sleepTime);
        } catch (final InterruptedException e) {
          // swap the world data a and b; a stays primary
          final var tmp = GamePanel.this.worldDataA;
          GamePanel.this.worldDataA = GamePanel.this.worldDataB;
          GamePanel.this.worldDataB = tmp;

          return;
        }
      }

      // swap the world data a and b; a stays primary
      final var tmp = GamePanel.this.worldDataA;
      GamePanel.this.worldDataA = GamePanel.this.worldDataB;
      GamePanel.this.worldDataB = tmp;

      // pass the new generation to the UI
      this.worldUI.draw(GamePanel.this.worldDataA);
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
      final CompletableFuture<Void>[] allFutures =
          Arrays.stream(this.calcTickParts)
              .map(x -> CompletableFuture.runAsync(x))
              .toArray(size -> new CompletableFuture[size]);
      final CompletableFuture<Void> allDoneFuture = CompletableFuture.allOf(allFutures);
      try {
        allDoneFuture.get();
      } catch (final Exception e) {
        // ignore
        return;
      }

      // calculate time spend for this tick
      final var tickTime = System.nanoTime() - start;
      this.tpsLabel.add(tickTime);

      // sleep if the tick was too fast
      final var sleepTime = this.minTickTime - tickTime / 1000000L;
      if (sleepTime > 0L) {
        try {
          Thread.sleep(sleepTime);
        } catch (final InterruptedException e) {
          // swap the world data a and b; a stays primary
          final var tmp = GamePanel.this.worldDataA;
          GamePanel.this.worldDataA = GamePanel.this.worldDataB;
          GamePanel.this.worldDataB = tmp;

          return;
        }
      }

      // swap the world data a and b; a stays primary
      final var tmp = GamePanel.this.worldDataA;
      GamePanel.this.worldDataA = GamePanel.this.worldDataB;
      GamePanel.this.worldDataB = tmp;

      // pass the new generation to the UI
      this.worldUI.draw(GamePanel.this.worldDataA);
    }
  }
}