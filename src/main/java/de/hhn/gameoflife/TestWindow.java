package de.hhn.gameoflife;

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.Semaphore;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

/** A window to test the games core logic. */
public class TestWindow extends JInternalFrame implements Disposable {

  private final World world;
  private final Stack<IntSet> testsIn = new Stack<>();
  private final Stack<Boolean> testsOut = new Stack<>();
  private final int testOutIndex = 5;
  private final DIContainer diContainer = new DIContainer();
  private boolean disposed = false;

  public TestWindow() {
    super("Test", true, true, true, true);

    final var diContainer = new DIContainer();
    diContainer.addSingleton(this);
    diContainer.addSingleton(new Settings(4, 4));
    diContainer.addSingleton(World.class);
    diContainer.addSingleton(
        new Drawable<IntSet>() {
          public void draw(final IntSet ignore) {
            // do nothing
          }
        });
    diContainer.addSingleton(TPS.class);
    diContainer.addSingleton(new Semaphore(1));
    this.world = diContainer.get(World.class);

    {
      // input
      final var in = new IntSet(16);
      this.testsIn.push(in);
      // 0000
      // 1000
      // 0110
      // 0000
      in.add(4);
      in.add(9);
      in.add(10);
      // expected output
      this.testsOut.push(true);
    }
    {
      // input
      final var in = new IntSet(16);
      this.testsIn.push(in);
      // 0000
      // 0100
      // 0010
      // 0000
      in.add(5);
      in.add(10);
      // expected output
      this.testsOut.push(false);
    }
    {
      final var in = new IntSet(16);
      this.testsIn.push(in);
      // 0100
      // 1100
      // 0010
      // 0000
      in.add(1);
      in.add(4);
      in.add(5);
      in.add(10);
      // expected output
      this.testsOut.push(true);
    }
    final var in = new IntSet(16);
    this.testsIn.push(in);
    // 1110
    // 0110
    // 1000
    // 0000
    in.add(0);
    in.add(1);
    in.add(2);
    in.add(5);
    in.add(6);
    in.add(8);
    // expected output
    this.testsOut.push(false);

    this.pack();
    this.show();

    SwingUtilities.invokeLater(this::runTests);
  }

  @Override
  public void dispose() {
    if (this.disposed) {
      return;
    }
    this.disposed = true;
    this.world.dispose();
    this.diContainer.dispose();
    super.dispose();
  }

  private void runTests() {
    final var errTxt = new StringBuilder();
    var i = 0;
    errTxt.append("<html>");
    errTxt.append(
        String.format(
            "<p>JVM: %s %s</p>",
            System.getProperty("java.vm.name"), System.getProperty("java.vm.version")));
    errTxt.append(String.format("Vendor: %s</p>", System.getProperty("java.vendor")));
    errTxt.append(
        String.format(
            "<p>OS: %s %s</p>", System.getProperty("os.name"), System.getProperty("os.version")));
    errTxt.append(String.format("<p>Arch: %s</p>", System.getProperty("os.arch")));
    errTxt.append("<hr>");
    while (!this.testsIn.isEmpty() && !this.testsOut.isEmpty()) {
      final var in = this.testsIn.pop();
      final var expectedOut = this.testsOut.pop();
      this.world.overwriteWorldData(in);
      this.world.calcTick();
      final var realOut = this.world.getWorldDataA().contains(this.testOutIndex);
      errTxt.append(
          String.format(
              "<p><b>Test %d: %s</b></p>", i, realOut == expectedOut ? "PASSED" : "FAILED"));
      errTxt.append(String.format("<p>Input: %s</p>", this.displayWorldData(in)));
      errTxt.append(String.format("<p>Expected: %s</p>", expectedOut));
      errTxt.append(
          String.format(
              "<p>Result: %s</p>",
              realOut != expectedOut ? this.displayWorldData(this.world.getWorldDataA()) : realOut));
      ++i;
    }
    errTxt.append("</html>");
    final var errLabel = new JLabel();
    this.setContentPane(errLabel);
    this.world.dispose();
    this.diContainer.dispose();
    errLabel.setText(errTxt.toString());
    this.pack();
  }

  private String displayWorldData(final IntSet bs) {
    final var sb = new StringBuilder();
    for (var y = 0; y < 4; ++y) {
      sb.append("<p>");
      for (var x = 0; x < 4; ++x) {
        final var i = y * 4 + x;
        if (i == this.testOutIndex) {
          sb.append("<b>");
          sb.append(bs.contains(i) ? '1' : '0');
          sb.append("</b>");
        } else {
          sb.append(bs.contains(i) ? '1' : '0');
        }
      }
      sb.append("</p>");
    }
    return sb.toString();
  }
}