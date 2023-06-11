package de.hhn.gameoflife.ui;

import de.hhn.gameoflife.control_iface.Disposable;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;

/** A window to test the games core logic. */
public class InfoWindow extends JInternalFrame implements Disposable {

  private boolean disposed = false;

  public InfoWindow() {
    super("Info", true, true, true, true);
    String infoTxt =
        "<html>"
            + String.format(
                "<p>JVM: %s %s</p>",
                System.getProperty("java.vm.name"), System.getProperty("java.vm.version"))
            + String.format("Vendor: %s</p>", System.getProperty("java.vendor"))
            + String.format(
                "<p>OS: %s %s</p>", System.getProperty("os.name"), System.getProperty("os.version"))
            + String.format("<p>Arch: %s</p>", System.getProperty("os.arch"))
            + "</html>";
    final var infoLabel = new JLabel();
    this.setContentPane(infoLabel);
    infoLabel.setText(infoTxt);
    this.pack();
    this.show();
  }

  @Override
  public void dispose() {
    if (this.disposed) {
      return;
    }
    this.disposed = true;
    super.dispose();
  }
}