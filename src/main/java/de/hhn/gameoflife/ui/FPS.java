package de.hhn.gameoflife.ui;

import de.hhn.gameoflife.logic.Settings;
import javax.swing.JLabel;

/** component to render the current frame time */
public class FPS extends JLabel {
  private final int size;
  private final int sizeMinusOne;
  private final double div;
  private final double[] timings;
  private int index = 0;

  public FPS(final Settings settings) {
    this.setText("measuring...");
    final var avg = (settings.worldWidth() >> 1) + (settings.worldHeight() >> 1);
    this.size = 64 - (avg / 140);
    this.sizeMinusOne = this.size - 1;
    this.div = this.size * 1_000_000d;
    this.timings = new double[this.size];
  }

  /** add a new timing in nanoseconds */
  public void add(final double time) {
    this.timings[this.index] = time;
    if (this.index == this.sizeMinusOne) {
      this.index = 0;
      this.setText(String.format("%.2f FPS", this.get()));
    } else {
      ++this.index;
    }
  }

  /** calculate the average fps */
  public double get() {
    double sum = 0d;
    for (final double timing : this.timings) {
      sum += timing;
    }
    final double avgFrameTime = sum / this.div;
    return 1_000d / avgFrameTime;
  }
}