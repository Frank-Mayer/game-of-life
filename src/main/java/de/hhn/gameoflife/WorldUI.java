package de.hhn.gameoflife;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.BitSet;
import javax.swing.JPanel;

/** component to render the world */
public class WorldUI extends JPanel {

  private final int worldSize;
  private final int logWorldWidth;
  private final int worldWidthMinusOne;
  private final BufferedImage buffer;
  private int colorAlive = 0xFFFFFF;
  private int colorDead = 0x000000;
  private BitSet worldData;

  public WorldUI(final BitSet worldData, final int worldWidth, final int worldHeight) {
    this.worldData = (BitSet) worldData.clone();
    this.logWorldWidth = (int) (Math.log(worldWidth) / Math.log(2));
    this.worldWidthMinusOne = worldWidth - 1;
    this.worldSize = worldWidth * worldHeight;
    this.buffer = new BufferedImage(worldWidth, worldHeight, BufferedImage.TYPE_INT_RGB);
    this.draw();
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
    this.buffer.flush();
    this.buffer.getGraphics().dispose();
    this.worldData = null;
  }

  /** draw the given worlds state */
  public void draw(final BitSet newData) {
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
    int i;
    int x;
    int y;
    for (i = 0; i < this.worldSize; ++i) {
      x = i & this.worldWidthMinusOne; // x = i % this.worldWidth;
      y = i >> this.logWorldWidth; // i / this.worldWidth;
      this.buffer.setRGB(x, y, this.worldData.get(i) ? this.colorAlive : this.colorDead);
    }
    g.drawImage(this.buffer, 0, 0, this.getWidth(), this.getHeight(), null);
  }

  /** get the current worlds image */
  public BufferedImage getImage() {
    return this.buffer;
  }
}