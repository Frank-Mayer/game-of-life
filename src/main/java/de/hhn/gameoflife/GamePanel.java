package de.hhn.gameoflife;

import static de.hhn.gameoflife.State.useState;

import java.awt.Color;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
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

public class GamePanel extends JPanel {

  private final int worldWidth;
  private final int worldHeight;
  private final int worldSize;
  private final int worldSizeMinusWorldWidth;
  private final int worldSizePlusWorldWidth;
  private final int worldSizeMinusOne;
  private final WorldUI worldUI;
  private final TPS tpsLabel;

  private final ScheduledExecutorService sheduler;

  // # world data
  // ## primary world data
  private BitSet worldDataA;
  // ## secondary world data; temporary storage for the next generation
  private BitSet worldDataB;

  private boolean paused = true;

  private long minTickTime = 0;

  private Runnable[] calcTickParts;
  private final Object lock = new Object();

  public GamePanel(final int width, final int height) {

    // # initialize ui
    this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    // ## tick time label
    this.tpsLabel = new TPS();
    this.add(this.tpsLabel);
    // ## min tick time slider (delay)
    final var minTickTimeLabel = new JLabel("Min Tick Time (0ms)");
    this.add(minTickTimeLabel);
    final var minTickTimeSlider = new JSlider(0, 500, 0);
    minTickTimeSlider.addChangeListener(
        e -> {
          this.minTickTime = minTickTimeSlider.getValue();
          minTickTimeLabel.setText(String.format("Min Tick Time (%d ms)", this.minTickTime));
        });
    minTickTimeSlider.setMajorTickSpacing(100);
    minTickTimeSlider.setMinorTickSpacing(10);
    minTickTimeSlider.setPaintTicks(true);
    this.add(minTickTimeSlider);

    // # initialize the "world"
    this.worldWidth = width;
    this.worldHeight = height;
    this.worldSize = width * height;
    this.worldSizeMinusWorldWidth = this.worldSize - this.worldWidth;
    this.worldSizePlusWorldWidth = this.worldSize + this.worldWidth;
    this.worldSizeMinusOne = this.worldSize - 1;
    this.worldDataA = new BitSet(this.worldSize);
    this.worldDataB = new BitSet(this.worldSize);
    final var rand = new Random();
    for (int i = 0; i < this.worldSize; ++i) {
      // ## randomly decide if the cell is alive or dead
      final var alive = rand.nextBoolean();
      this.worldDataA.set(i, alive);
      this.worldDataB.set(i, alive);
    }
    // ## world ui to display the world
    this.worldUI = new WorldUI(this.worldDataA, width, height);
    this.add(this.worldUI);
    // ### add mouse listener to toggle cells
    final var drawNewState = useState(false);
    final var wasPaused = useState(false);
    this.worldUI.addMouseListener(
        new MouseListener() {
          @Override
          public void mouseClicked(final MouseEvent e) {}

          @Override
          public void mousePressed(final MouseEvent e) {
            wasPaused.set(GamePanel.this.paused);
            GamePanel.this.paused = true;
            synchronized (GamePanel.this.lock) {
              drawNewState.set(GamePanel.this.worldUI.togglePoint(e.getPoint()));
              GamePanel.this.worldUI.draw();
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
            synchronized (GamePanel.this.lock) {
              worldUI.togglePoint(e.getPoint(), drawNewState.get());
              GamePanel.this.worldUI.draw();
            }
          }

          @Override
          public void mouseMoved(final MouseEvent e) {}
        });

    this.sheduler = Executors.newSingleThreadScheduledExecutor();
    if (this.worldSize >= 1_048_576) { // 1048576 = 1024 * 1024
      final var partsCount = Math.max(1, Runtime.getRuntime().availableProcessors() / 2);
      System.out.println("partsCount = " + partsCount);
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
      // # start the game loop
      this.sheduler.scheduleWithFixedDelay(this::tickAsync, 500, 1, TimeUnit.MILLISECONDS);
    } else {
      System.out.println("single threaded");
      // # start the game loop
      this.sheduler.scheduleWithFixedDelay(this::tickSync, 500, 1, TimeUnit.MILLISECONDS);
    }
  }

  // # free resources
  public void dispose() {
    this.sheduler.shutdownNow();
    this.worldDataA = null;
    this.worldDataB = null;
    this.worldUI.dispose();
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

  // # calculate next generation
  public void calcTick() {
    calcTick(0, this.worldSize);
  }

  public void calcTick(final int start, final int end) {
    // ## initialize local variables
    int i = start;
    int iMinusOne = start - 1;
    int iPlusOne = start + 1;
    int livingNeighbors;
    int neighborIndex;
    boolean alive;

    // ## check all cells in the world
    while (i < end) {
      // ### is this cell currently alive?
      alive = GamePanel.this.worldDataA.get(i);

      // ### check all neighbors
      livingNeighbors = 0;
      // #### 1: north
      neighborIndex =
          i + GamePanel.this.worldSizeMinusWorldWidth & GamePanel.this.worldSizeMinusOne;
      if (GamePanel.this.worldDataA.get(neighborIndex)) {
        ++livingNeighbors;
      }
      // #### 2: north-east
      neighborIndex =
          iPlusOne + GamePanel.this.worldSizeMinusWorldWidth & GamePanel.this.worldSizeMinusOne;
      if (GamePanel.this.worldDataA.get(neighborIndex)) {
        ++livingNeighbors;
      }
      // #### 3: east
      neighborIndex = iPlusOne & GamePanel.this.worldSizeMinusOne;
      if (GamePanel.this.worldDataA.get(neighborIndex)) {
        ++livingNeighbors;
      }
      // #### 4: south-east
      neighborIndex = iPlusOne + GamePanel.this.worldWidth & GamePanel.this.worldSizeMinusOne;
      if (GamePanel.this.worldDataA.get(neighborIndex)) {
        ++livingNeighbors;
      }
      // #### 5: south
      neighborIndex = i + GamePanel.this.worldWidth & GamePanel.this.worldSizeMinusOne;
      if (GamePanel.this.worldDataA.get(neighborIndex)) {
        ++livingNeighbors;
      }
      // #### 6: south-west
      neighborIndex =
          iMinusOne + GamePanel.this.worldWidth + GamePanel.this.worldSize
              & GamePanel.this.worldSizeMinusOne;
      if (GamePanel.this.worldDataA.get(neighborIndex)) {
        ++livingNeighbors;
      }
      // #### 7: west
      neighborIndex = iMinusOne + GamePanel.this.worldSize & GamePanel.this.worldSizeMinusOne;
      if (GamePanel.this.worldDataA.get(neighborIndex)) {
        ++livingNeighbors;
      }
      // #### 8: north-west
      neighborIndex =
          iMinusOne - GamePanel.this.worldSizePlusWorldWidth & GamePanel.this.worldSizeMinusOne;
      if (GamePanel.this.worldDataA.get(neighborIndex)) {
        ++livingNeighbors;
      }

      // #### alive1 = alive0 ? (2 or 3 neighbors) : (3 neighbors)
      GamePanel.this.worldDataB.set(i, livingNeighbors == 3 || alive && livingNeighbors == 2);

      // #### next cell
      iMinusOne = i;
      i = iPlusOne++;
    }
  }

  public boolean togglePaused() {
    return this.paused = !this.paused;
  }

  // # clear the world
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

  // # load world data from an image file
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
      } catch (final IOException e) {
        Alert.show("Error", e.getMessage(), this.worldUI);
        this.paused = wasPaused;
        return;
      }
      BufferedImage resized =
          new BufferedImage(this.worldWidth, this.worldHeight, BufferedImage.TYPE_INT_RGB);
      final var g = resized.createGraphics();
      g.drawImage(img, 0, 0, this.worldWidth, this.worldHeight, null);
      // ## apply filters
      resized = Dithering.floydSteinberg(Dithering.grayScale(resized));
      // ## write pixels into world data
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

  private void tickSync() {
    // ## check if the game is running
    if (this.paused) {
      return;
    }

    synchronized (this.lock) {
      // ## save start time
      final var start = System.nanoTime();

      // ## calculate next generation
      calcTick(0, this.worldSize);

      // ## calculate time spend for this tick
      final var tickTime = System.nanoTime() - start;
      this.tpsLabel.add(tickTime);

      // ## sleep
      final var sleepTime = this.minTickTime - tickTime / 1000000L;
      if (sleepTime > 0L) {
        try {
          Thread.sleep(sleepTime);
        } catch (final Exception e) {
          // ### ignore
        }
      }

      // ## swap the world data a and b; a stays primary
      final var tmp = GamePanel.this.worldDataA;
      GamePanel.this.worldDataA = GamePanel.this.worldDataB;
      GamePanel.this.worldDataB = tmp;

      // ## pass the new generation to the UI
      this.worldUI.draw(GamePanel.this.worldDataA);
    }
  }

  private void tickAsync() {
    // ## check if the game is running
    if (this.paused) {
      return;
    }

    synchronized (this.lock) {
      // ## save start time
      final var start = System.nanoTime();

      // ## calculate next generation
      final CompletableFuture<Void>[] allFutures =
          Arrays.stream(this.calcTickParts)
              .map(x -> CompletableFuture.runAsync(x))
              .toArray(size -> new CompletableFuture[size]);
      final CompletableFuture<Void> allDoneFuture = CompletableFuture.allOf(allFutures);
      try {
        allDoneFuture.get();
      } catch (final Exception e) {
        e.printStackTrace();
        return;
      }

      // ## calculate time spend for this tick
      final var tickTime = System.nanoTime() - start;
      this.tpsLabel.add(tickTime);

      // ## sleep
      final var sleepTime = this.minTickTime - tickTime / 1000000L;
      if (sleepTime > 0L) {
        try {
          Thread.sleep(sleepTime);
        } catch (final Exception e) {
          // ### ignore
        }
      }

      // ## swap the world data a and b; a stays primary
      final var tmp = GamePanel.this.worldDataA;
      GamePanel.this.worldDataA = GamePanel.this.worldDataB;
      GamePanel.this.worldDataB = tmp;

      // ## pass the new generation to the UI
      this.worldUI.draw(GamePanel.this.worldDataA);
    }
  }
}