package de.hhn.gameoflife;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.Set;
import javax.swing.JPanel;

/** component to render the world */
public class WorldUI extends JPanel implements Drawable<IntSet> {

  private final int worldSize;
  private final int logWorldWidth;
  private final int worldWidthMinusOne;
  private final BufferedImage buffer;
  private int colorAlive = 0xFFFFFF;
  private int colorDead = 0x000000;
  private IntSet worldData;
  private boolean disposed;

  public WorldUI(final Settings settings) {
    this.logWorldWidth = Utils.log2(settings.worldWidth());
    this.worldWidthMinusOne = settings.worldWidth() - 1;
    this.worldSize = settings.worldWidth() * settings.worldHeight();
    this.buffer =
        new BufferedImage(
            settings.worldWidth(), settings.worldHeight(), BufferedImage.TYPE_INT_RGB);
  }

  public Color getAliveColor() {
    return new Color(this.colorAlive);
  }

  public void setAliveColor(final Color color) {
    this.colorAlive = color.getRGB();
    this.draw();
  }

  public Color getDeadColor() {
    return new Color(this.colorDead);
  }

  public void setDeadColor(final Color color) {
    this.colorDead = color.getRGB();
    this.draw();
  }

  @Override
  public void paintComponent(final Graphics g) {
    this.draw(g);
  }

  @Override
  public void paint(final Graphics g) {
    this.draw(g);
  }

  /** free resources */
  public void dispose() {
    if (this.disposed) {
      return;
    }
    this.disposed = true;
    this.buffer.flush();
    this.buffer.getGraphics().dispose();
    this.worldData = null;
  }

  /** draw the given worlds state */
  public void draw(final IntSet newData) {
    this.worldData = newData;
    this.draw();
  }

  /** draw the current worlds state */
  public void draw() {
    final var g = this.getGraphics();
    if (g == null) {
      return;
    }
    this.draw(g);
  }

  /** draw the current worlds state using the given graphics object */
  public void draw(final Graphics g) {
    if (this.worldData == null) {
      return;
    }

    int i;
    int x;
    int y;
    for (i = 0; i < this.worldSize; ++i) {
      x = i & this.worldWidthMinusOne; // x = i % this.worldWidth;
      y = i >> this.logWorldWidth; // i / this.worldWidth;
      this.buffer.setRGB(x, y, this.worldData.contains(i) ? this.colorAlive : this.colorDead);
    }
    g.drawImage(this.buffer, 0, 0, this.getWidth(), this.getHeight(), null);
  }

  /** get the current worlds image */
  public BufferedImage getImage() {
    return this.buffer;
  }
}