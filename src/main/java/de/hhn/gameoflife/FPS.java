package de.hhn.gameoflife;

import javax.swing.JLabel;

/** component to render the current tick time */
public class FPS extends JLabel {

  private static final int SIZE = 32;
  private static final int SIZE_MINUS_ONE = FPS.SIZE - 1;
  private static final double DIV = FPS.SIZE * 1_000_000d;

  private final double[] timings = new double[FPS.SIZE];
  private int index = 0;

  public FPS() {
    this.setText("measuring...");
  }

  /** add a new timing in nanoseconds */
  public void add(final double time) {
    this.timings[this.index] = time;
    if (this.index == FPS.SIZE_MINUS_ONE) {
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
    final double avgFrameTime = sum / FPS.DIV;
    return 1_000d / avgFrameTime;
  }
}