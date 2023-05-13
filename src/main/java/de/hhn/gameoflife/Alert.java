package de.hhn.gameoflife;

import java.awt.BorderLayout;
import java.awt.Component;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;

public class Alert extends JDialog {
  private Alert(final String title, final String message, final Component c) {
    super();
    this.setLayout(new BorderLayout());
    this.setTitle(title);
    this.setModal(true);
    this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    this.add(new JLabel(message), BorderLayout.CENTER);
    this.add(
        new JButton("OK") {
          {
            addActionListener(e -> Alert.this.dispose());
          }
        },
        BorderLayout.SOUTH);

    this.pack();
  }

  public static void show(final String title, final String message, final Component c) {
    final var alert = new Alert(title, message, c);
    alert.setLocationRelativeTo(c);
    alert.setVisible(true);
  }
}