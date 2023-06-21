package de.hhn.gameoflife.ui;

import de.hhn.gameoflife.control_iface.Drawable;
import de.hhn.gameoflife.data_structures.IntSet;
import de.hhn.gameoflife.logic.Settings;
import de.hhn.gameoflife.logic.Snake;
import de.hhn.gameoflife.util.Utils;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import javax.swing.JPanel;

/** component to render the world */
public class WorldUI extends JPanel implements Drawable<IntSet> {

  private final int worldSize;
  private final int logWorldWidth;
  private final int worldWidthMinusOne;
  private final BufferedImage worldBuffer;
  private final BufferedImage masterBuffer;
  private final BufferedImage overlayBuffer;
  private int colorAlive = 0xffffff;
  private int colorDead = 0x000000;
  private boolean disposed;
  private final int worldWidth;
  private final int worldHeight;

  public WorldUI(final Settings settings) {
    this.logWorldWidth = Utils.log2(settings.worldWidth());
    this.worldWidthMinusOne = settings.worldWidth() - 1;
    this.worldSize = settings.worldWidth() * settings.worldHeight();
    this.worldWidth = settings.worldWidth();
    this.worldHeight = settings.worldHeight();
    this.worldBuffer =
        new BufferedImage(
            settings.worldWidth(), settings.worldHeight(), BufferedImage.TYPE_INT_RGB);
    this.overlayBuffer =
        new BufferedImage(
            settings.worldWidth(), settings.worldHeight(), BufferedImage.TYPE_INT_ARGB);
    this.masterBuffer =
        new BufferedImage(
            settings.worldWidth(), settings.worldHeight(), BufferedImage.TYPE_INT_RGB);
    this.compose();
  }

  public Color getAliveColor() {
    return new Color(this.colorAlive);
  }

  public void setAliveColor(final Color color) {
    this.colorAlive = color.getRGB();
  }

  public Color getDeadColor() {
    return new Color(this.colorDead);
  }

  public void setDeadColor(final Color color) {
    this.colorDead = color.getRGB();
  }

  @Override
  public void paintComponent(final Graphics g) {
    g.drawImage(this.masterBuffer, 0, 0, this.getWidth(), this.getHeight(), null);
  }

  @Override
  public void paint(final Graphics g) {
    g.drawImage(this.masterBuffer, 0, 0, this.getWidth(), this.getHeight(), null);
  }

  /** free resources */
  public void dispose() {
    if (this.disposed) {
      return;
    }
    this.disposed = true;
    this.worldBuffer.flush();
    this.worldBuffer.getGraphics().dispose();
    this.masterBuffer.flush();
    this.masterBuffer.getGraphics().dispose();
    this.overlayBuffer.flush();
    this.overlayBuffer.getGraphics().dispose();
  }

  public void draw() {
    final var g = this.getGraphics();
    if (g == null) {
      return;
    }
    this.compose();
    g.drawImage(this.masterBuffer, 0, 0, this.getWidth(), this.getHeight(), null);
  }

  @Override
  public void set(final IntSet data) {
    int i;
    int x;
    int y;
    for (i = 0; i < this.worldSize; ++i) {
      x = i & this.worldWidthMinusOne; // x = i % this.worldWidth;
      y = i >> this.logWorldWidth; // i / this.worldWidth;
      this.worldBuffer.setRGB(x, y, data.contains(i) ? this.colorAlive : this.colorDead);
    }
    this.draw();
  }

  /** get the current worlds image */
  public BufferedImage getImage() {
    return this.masterBuffer;
  }

  @Override
  public void set(final int index, final boolean alife) {
    this.worldBuffer.setRGB(
        index & this.worldWidthMinusOne,
        index >> this.logWorldWidth,
        alife ? this.colorAlive : this.colorDead);
  }

  @Override
  public void compose() {
    final var g = this.masterBuffer.getGraphics();
    g.drawImage(this.worldBuffer, 0, 0, null);
    g.drawImage(this.overlayBuffer, 0, 0, null);
    g.dispose();
  }

  public void snake(final Snake snake) {
    snake.onChange(this::drawSnake);
  }

  public synchronized void drawSnake(final Iterable<Integer> positions) {
    for(int y = 0; y != this.worldHeight; ++y) {
      for(int x = 0; x != this.worldWidth; ++x) {
        this.overlayBuffer.setRGB(x, y, Color.TRANSLUCENT);
      }
    }
    for (final var position : positions) {
      final var x = position & this.worldWidthMinusOne;
      final var y = position >> this.logWorldWidth;
      this.overlayBuffer.setRGB(x, y, Color.RED.getRGB());
    }
    this.draw();
  }
}