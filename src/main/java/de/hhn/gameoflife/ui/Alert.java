package de.hhn.gameoflife.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;

/** A simple alert dialog. */
public class Alert extends JDialog {
  public static void show(final String title, final String message, final Component c) {
    final var alert = new Alert(title, message, c);
    alert.setLocationRelativeTo(c);
    alert.setVisible(true);
  }

  private Alert(final String title, final String message, final Component c) {
    super();
    this.setLayout(new BorderLayout());
    this.setTitle(title);
    this.setModal(true);
    this.setResizable(false);
    this.setMinimumSize(new Dimension(250, 100));
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
}