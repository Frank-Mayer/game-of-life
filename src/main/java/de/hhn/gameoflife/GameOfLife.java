package de.hhn.gameoflife;

import de.hhn.gameoflife.logic.Snake;
import de.hhn.gameoflife.ui.MyMenuBar;
import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.UIManager;

public class GameOfLife {
  public static void main(final String[] args) {
    // ensure that the style is the same on all platforms
    try {
      System.setProperty("apple.laf.useScreenMenuBar", "true");
      System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Game of Life");
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    } catch (final Exception ignore) {
      // dann halt nicht 😒
    }

    // create the window
    final var window = new JFrame();
    window.setTitle("Game of Life");
    window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    window.addKeyListener(new Snake.SnakeKeyListener());
    window.setFocusable(true);

    // Create a JDesktopPane for use with JInternalFrames.
    final var deskPane = new JDesktopPane();
    window.add(deskPane);

    // add a menu bar
    final var menuBar = new MyMenuBar(deskPane);
    window.setJMenuBar(menuBar);

    window.setVisible(true);
    window.setExtendedState(window.getExtendedState() | JFrame.MAXIMIZED_BOTH);
  }
}