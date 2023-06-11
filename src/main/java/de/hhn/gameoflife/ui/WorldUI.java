package de.hhn.gameoflife.ui;

import de.hhn.gameoflife.control_iface.Drawable;
import de.hhn.gameoflife.data_structures.IntSet;
import de.hhn.gameoflife.logic.Settings;
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
  private final BufferedImage buffer;
  private int colorAlive = 0xffffff;
  private int colorDead = 0x000000;
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
  }

  public Color getDeadColor() {
    return new Color(this.colorDead);
  }

  public void setDeadColor(final Color color) {
    this.colorDead = color.getRGB();
  }

  @Override
  public void paintComponent(final Graphics g) {
    g.drawImage(this.buffer, 0, 0, this.getWidth(), this.getHeight(), null);
  }

  @Override
  public void paint(final Graphics g) {
    g.drawImage(this.buffer, 0, 0, this.getWidth(), this.getHeight(), null);
  }

  /** free resources */
  public void dispose() {
    if (this.disposed) {
      return;
    }
    this.disposed = true;
    this.buffer.flush();
    this.buffer.getGraphics().dispose();
  }

  public void draw() {
    final var g = this.getGraphics();
    if (g == null) {
      return;
    }
    g.drawImage(this.buffer, 0, 0, this.getWidth(), this.getHeight(), null);
  }

  public void set(final IntSet data) {
    int i;
    int x;
    int y;
    for (i = 0; i < this.worldSize; ++i) {
      x = i & this.worldWidthMinusOne; // x = i % this.worldWidth;
      y = i >> this.logWorldWidth; // i / this.worldWidth;
      this.buffer.setRGB(x, y, data.contains(i) ? this.colorAlive : this.colorDead);
    }
  }

  /** get the current worlds image */
  public BufferedImage getImage() {
    return this.buffer;
  }

  @Override
  public void set(int index, boolean alife) {
    this.buffer.setRGB(
        index & this.worldWidthMinusOne,
        index >> this.logWorldWidth,
        alife ? this.colorAlive : this.colorDead);
  }
}