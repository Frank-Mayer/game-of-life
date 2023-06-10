package de.hhn.gameoflife;

import javax.swing.JLabel;

/** component to render the current tick time */
public class FPS extends JLabel {

  private final double[] timings = new double[32];
  private int index = 0;

  public FPS() {
    this.setText("measuring...");
  }

  /** add a new timing in nanoseconds */
  public void add(final double time) {
    this.timings[this.index] = time;
    if (this.index == 31) { // if (this.index == this.timings.length - 1) {
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
    final double avgFrameTime = sum / 32000000d; // sum / (this.timings.length * 1000000d);
    return 1000d / avgFrameTime;
  }
}