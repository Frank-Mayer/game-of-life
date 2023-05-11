package de.hhn.gameoflife;

import javax.swing.JLabel;

// # component to render the TPS (ticks per second)
public class TPS extends JLabel {

  private final double[] timings = new double[32];
  private int index = 0;

  public TPS() {
    this.setText("Tick Time: Unknown");
  }

  // ## add a new timing in nanoseconds
  public void add(final double time) {
    this.timings[this.index] = time;
    if (this.index == 31) { // if (this.index == this.timings.length - 1) {
      this.index = 0;
      this.setText(String.format("Tick Time: %.4fms", this.get()));
    } else {
      ++this.index;
    }
  }

  // ## calculate the average time spend for a tick in milliseconds
  public double get() {
    double sum = 0d;
    for (final double timing : this.timings) {
      sum += timing;
    }
    return sum / 32000000d; // sum / (this.timings.length * 1000000d);
  }
}