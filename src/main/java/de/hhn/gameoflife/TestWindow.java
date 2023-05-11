package de.hhn.gameoflife;

import java.util.BitSet;
import java.util.Stack;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

public class TestWindow extends JInternalFrame {

  private final GamePanel gamePanel;
  private final Stack<BitSet> testsIn = new Stack<>();
  private final Stack<Boolean> testsOut = new Stack<>();
  private final int testOutIndex = 5;

  public TestWindow() {
    super("Test", true, true, true, true);

    this.gamePanel = new GamePanel(4, 4);
    {
      // input
      final var in = new BitSet(16);
      this.testsIn.push(in);
      // 0000
      // 1000
      // 0110
      // 0000
      in.set(0, 4, false);
      in.set(4, true);
      in.set(5, 9, false);
      in.set(9, true);
      in.set(10, true);
      in.set(11, 16, false);
      // expected output
      this.testsOut.push(true);
    }
    {
      // input
      final var in = new BitSet(16);
      this.testsIn.push(in);
      // 0000
      // 0100
      // 0010
      // 0000
      in.set(5);
      in.set(10);
      // expected output
      this.testsOut.push(false);
    }
    {
      final var in = new BitSet(16);
      this.testsIn.push(in);
      // 0100
      // 1100
      // 0010
      // 0000
      in.set(1);
      in.set(4);
      in.set(5);
      in.set(10);
      // expected output
      this.testsOut.push(true);
    }
    final var in = new BitSet(16);
    this.testsIn.push(in);
    // 1110
    // 0110
    // 1000
    // 0000
    in.set(0);
    in.set(1);
    in.set(2);
    in.set(5);
    in.set(6);
    in.set(8);
    // expected output
    this.testsOut.push(false);

    this.setContentPane(this.gamePanel);
    this.pack();
    this.show();

    SwingUtilities.invokeLater(this::runTests);
  }

  private void runTests() {
    final var errTxt = new StringBuilder();
    var i = 0;
    errTxt.append("<html>");
    while (!this.testsIn.isEmpty() && !this.testsOut.isEmpty()) {
      final var in = this.testsIn.pop();
      final var expectedOut = this.testsOut.pop();
      this.gamePanel.overwriteWorldData(in);
      this.gamePanel.calcTick();
      final var realOut = this.gamePanel.getWorldDataB().get(this.testOutIndex);
      errTxt.append(
          String.format(
              "<p><b>Test %d: %s</b></p>", i, realOut == expectedOut ? "PASSED" : "FAILED"));
      errTxt.append(String.format("<p>Input: %s</p>", this.displayBitSet(in)));
      errTxt.append(String.format("<p>Expected: %s</p>", expectedOut));
      errTxt.append(
          String.format(
              "<p>Result: %s</p>",
              realOut != expectedOut
                  ? this.displayBitSet(this.gamePanel.getWorldDataB())
                  : realOut));
      ++i;
    }
    errTxt.append("</html>");
    final var errLabel = new JLabel();
    this.setContentPane(errLabel);
    this.gamePanel.dispose();
    errLabel.setText(errTxt.toString());
    this.pack();
  }

  private String displayBitSet(final BitSet bs) {
    final var sb = new StringBuilder();
    for (var y = 0; y < 4; ++y) {
      sb.append("<p>");
      for (var x = 0; x < 4; ++x) {
        final var i = y * 4 + x;
        if (i == this.testOutIndex) {
          sb.append("<b>");
          sb.append(bs.get(i) ? '1' : '0');
          sb.append("</b>");
        } else {
          sb.append(bs.get(i) ? '1' : '0');
        }
      }
      sb.append("</p>");
    }
    return sb.toString();
  }
}