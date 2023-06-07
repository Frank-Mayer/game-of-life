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
import java.util.concurrent.Semaphore;
import javax.imageio.ImageIO;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;

/**
 * The main game panel.
 *
 * <p>This class encapsulates a Game of Life. Independent of the Window management.
 */
public class GamePanel extends JPanel implements Disposable {

  private final World world;
  private final WorldUI worldUI;
  private final TPS tpsLabel;
  private int worldWidth;
  private int worldHeight;
  private final DIContainer diContainer = new DIContainer();
  private boolean disposed = false;
  private boolean drawing = true;
  private DrawingStyle ds = DrawingStyle.BLOCK;
  private final Semaphore worldDataSem = new Semaphore(1);

  public GamePanel(final int width, final int height) {
    this.diContainer.addSingleton(new Settings(width, height));
    this.diContainer.addSingleton(World.class);
    this.diContainer.addSingleton(WorldUI.class);
    this.diContainer.addSingleton(TPS.class);
    this.diContainer.addSingleton(this.worldDataSem);

    this.worldWidth = width;
    this.worldHeight = height;

    // initialize world
    this.world = this.diContainer.get(World.class);

    // initialize ui
    this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    // tick time label
    this.tpsLabel = this.diContainer.get(TPS.class);
    this.add(this.tpsLabel);
    // min tick time slider (delay)
    final var minTickTimeLabel =
        new JLabel(String.format("Min Tick Time (%sms)", this.world.getMinTickTime()));
    this.add(minTickTimeLabel);
    final var minTickTimeSlider = new JSlider(0, 500, this.world.getMinTickTime());
    minTickTimeSlider.setPaintLabels(true);
    minTickTimeSlider.setPaintTrack(true);
    minTickTimeSlider.setPaintTicks(true);
    minTickTimeSlider.setMajorTickSpacing(100);
    minTickTimeSlider.setMinorTickSpacing(10);
    minTickTimeSlider.addChangeListener(
        e -> {
          this.world.setMinTickTime(minTickTimeSlider.getValue());
          minTickTimeLabel.setText(
              String.format("Min Tick Time (%d ms)", this.world.getMinTickTime()));
        });
    minTickTimeSlider.setMajorTickSpacing(100);
    minTickTimeSlider.setMinorTickSpacing(10);
    minTickTimeSlider.setPaintTicks(true);
    this.add(minTickTimeSlider);

    // world ui to display the world
    this.worldUI = this.diContainer.get(WorldUI.class);
    this.add(this.worldUI);
    // add mouse listener to toggle cells
    final var drawNewState = useState(false);
    final var wasPaused = useState(false);
    final var relativeBoundingRect = new Rectangle(0, 0, 0, 0);
    this.worldUI.addMouseListener(
        new MouseListener() {
          @Override
          public void mouseClicked(final MouseEvent e) {
            if (GamePanel.this.drawing) {
              return;
            }
            try {
              GamePanel.this.worldDataSem.acquire();
              GamePanel.this.togglePoints(e.getPoint(), GamePanel.this.ds.getStructure());
            } catch (final InterruptedException interruptedException) {
            } finally {
              GamePanel.this.worldDataSem.release();
            }
            GamePanel.this.worldUI.draw(GamePanel.this.world.getWorldData());
          }

          @Override
          public void mousePressed(final MouseEvent e) {
            if (!GamePanel.this.drawing) {
              return;
            }
            try {
              GamePanel.this.worldDataSem.acquire();
              wasPaused.set(GamePanel.this.world.getPaused());
              GamePanel.this.world.setPaused(true);
              relativeBoundingRect.setSize(
                  GamePanel.this.worldUI.getWidth(), GamePanel.this.worldUI.getHeight());

              drawNewState.set(GamePanel.this.togglePoint(e.getPoint()));
              GamePanel.this.worldUI.draw(GamePanel.this.world.getWorldData());
            } catch (final InterruptedException interruptedException) {
              interruptedException.printStackTrace();
            } finally {
              GamePanel.this.worldDataSem.release();
            }
          }

          @Override
          public void mouseReleased(final MouseEvent e) {
            if (!drawing) {
              return;
            }
            GamePanel.this.world.setPaused(wasPaused.get());
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
            if (!drawing) {
              return;
            }
            // if point is not in the worldUI, do nothing
            if (!relativeBoundingRect.contains(e.getPoint())) {
              return;
            }

            GamePanel.this.togglePoint(e.getPoint(), drawNewState.get());
            GamePanel.this.worldUI.draw(GamePanel.this.world.getWorldData());
          }

          @Override
          public void mouseMoved(final MouseEvent e) {}
        });
  }

  public void setDrawingStyle(final DrawingStyle ds) {
    this.ds = ds;
  }

  public void setDrawing(final boolean value) {
    drawing = value;
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
    this.world.togglePoint(x, y, state);
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
    return this.world.togglePoint(x, y);
  }

  public void togglePoints(final Point point, final Boolean[][] structure) {
    final var cellWidth = (double) GamePanel.this.worldUI.getWidth() / (double) this.worldWidth;
    final var cellHeight = (double) GamePanel.this.worldUI.getHeight() / (double) this.worldHeight;
    final var xStart = (int) (point.x / cellWidth);
    final var yStart = (int) (point.y / cellHeight);
    for (var y = 0; y < structure.length; y++) {
      final var row = structure[y];
      for (var x = 0; x < row.length; x++) {
        final var state = row[x];
        this.world.togglePoint(x + xStart, y + yStart, state);
      }
    }
  }

  /** free resources */
  public void dispose() {
    if (this.disposed) {
      return;
    }
    this.disposed = true;
    this.worldUI.dispose();
    this.world.dispose();
    this.diContainer.dispose();
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

  /** load world data from an image file */
  public void load(final File imageFile) {
    try {
      this.worldDataSem.acquire();
      BufferedImage img;
      try {
        img = ImageIO.read(imageFile);
        if (img == null) {
          Alert.show("Error", "The selected file is not a valid image file.", this.worldUI);
          return;
        }
      } catch (final Exception e) {
        Alert.show("Error", e.getMessage(), this.worldUI);
        return;
      }
      final var resized =
          new BufferedImage(this.worldWidth, this.worldHeight, BufferedImage.TYPE_INT_RGB);
      final var g = resized.createGraphics();
      g.drawImage(img, 0, 0, this.worldWidth, this.worldHeight, null);
      // apply dithering filters
      Dithering.grayScale(resized);
      Dithering.floydSteinberg(resized);
      // write pixels into world data
      this.world.setDataFrom(resized);
      g.dispose();
    } catch (final InterruptedException e) {
      e.printStackTrace();
    } finally {
      this.worldDataSem.release();
    }
  }

  /** save world data to an image file */
  public void save(final File imageFile) {
    final var img = this.worldUI.getImage();
    final var fileName = imageFile.getName();
    final var dotIndex = fileName.lastIndexOf('.');
    final var ext =
        dotIndex == -1 || dotIndex == fileName.length() - 1
            ? "png"
            : fileName.substring(dotIndex + 1);
    try {
      ImageIO.write(img, ext, imageFile);
    } catch (final Exception e) {
      Alert.show("Error", e.getMessage(), this.worldUI);
    }
  }

  public void clear() {
    this.world.clear();
  }

  public boolean togglePaused() {
    return this.world.togglePaused();
  }

  public void setPaused(final boolean value) {
    this.world.setPaused(value);
  }
}