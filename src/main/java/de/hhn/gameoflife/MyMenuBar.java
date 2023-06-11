package de.hhn.gameoflife;

import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.util.HashMap;
import javax.swing.JColorChooser;
import javax.swing.JDesktopPane;
import javax.swing.JFileChooser;
import javax.swing.JInternalFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.filechooser.FileNameExtensionFilter;

/** A menu bar for the game. */
public class MyMenuBar extends JMenuBar {

  private static final FileNameExtensionFilter imageFileFilter =
      new FileNameExtensionFilter("Image files", "jpg", "jpeg", "png", "gif");

  /** create a menu bar for an internal frame */
  private static JMenuBar makeInternalFrameMenuBar(final JInternalFrame inFrame) {
    final var menuBar = new JMenuBar();

    // add a menu to control the internal frame
    final var ctrlMenu = new JMenu("Control");
    menuBar.add(ctrlMenu);

    // add a menu item to pause / resume the game
    final var pauseMenuItem = new JMenuItem("Start");
    pauseMenuItem.addActionListener(
        e -> {
          final var gol = (GamePanel) inFrame.getContentPane();
          pauseMenuItem.setText(gol.togglePaused() ? "Resume" : "Pause");
        });
    ctrlMenu.add(pauseMenuItem);

    // add menu item to load game state from image file
    final var loadMenuItem = new JMenuItem("Load Image");
    loadMenuItem.addActionListener(
        e -> {
          final var gol = (GamePanel) inFrame.getContentPane();
          final var fileChooser = new JFileChooser();
          fileChooser.setFileFilter(imageFileFilter);
          final var result = fileChooser.showOpenDialog(inFrame);
          if (result == JFileChooser.APPROVE_OPTION) {
            gol.load(fileChooser.getSelectedFile());
          }
        });
    ctrlMenu.add(loadMenuItem);

    // add menu item to save game state to image file
    final var saveMenuItem = new JMenuItem("Save Image");
    saveMenuItem.addActionListener(
        e -> {
          final var gol = (GamePanel) inFrame.getContentPane();
          final var fileChooser = new JFileChooser();
          fileChooser.setFileFilter(imageFileFilter);
          final var result = fileChooser.showSaveDialog(inFrame);
          if (result == JFileChooser.APPROVE_OPTION) {
            gol.save(fileChooser.getSelectedFile());
          }
        });
    ctrlMenu.add(saveMenuItem);

    // add menu item to cklear the game state
    final var clearMenuItem = new JMenuItem("Clear");
    clearMenuItem.addActionListener(
        e -> {
          final var gol = (GamePanel) inFrame.getContentPane();
          gol.clear();
        });
    ctrlMenu.add(clearMenuItem);

    // add a menu item to close the internal frame
    final var closeMenuItem = new JMenuItem("Quit");
    closeMenuItem.addActionListener(e -> inFrame.dispose());
    ctrlMenu.add(closeMenuItem);

    // add a menu to control the world style
    final var styleMenu = new JMenu("Style");
    menuBar.add(styleMenu);
    final var aliveColorMenuItem = new JMenuItem("Alive Color");
    aliveColorMenuItem.addActionListener(
        e -> {
          final var gol = (GamePanel) inFrame.getContentPane();
          final var color =
              JColorChooser.showDialog(inFrame, "Choose Alive Color", gol.getAliveColor());
          if (color != null) {
            gol.setAliveColor(color);
          }
        });
    styleMenu.add(aliveColorMenuItem);
    final var deadColorMenuItem = new JMenuItem("Dead Color");
    deadColorMenuItem.addActionListener(
        e -> {
          final var gol = (GamePanel) inFrame.getContentPane();
          final var color =
              JColorChooser.showDialog(inFrame, "Choose Dead Color", gol.getDeadColor());
          if (color != null) {
            gol.setDeadColor(color);
          }
        });
    styleMenu.add(deadColorMenuItem);

    final var drawingModeMenu = new JMenu("Drawing Mode");
    menuBar.add(drawingModeMenu);

    final var penMenuItem = new JMenuItem("Pen");
    penMenuItem.addActionListener(
        e -> {
          final var gol = (GamePanel) inFrame.getContentPane();
          gol.setDrawing(true);
        });

    drawingModeMenu.add(penMenuItem);

    final var categories = new HashMap<DrawingStyleCategory, JMenu>();
    for (final var drawingStyle : DrawingStyle.values()) {
      final var dsmi = new JMenuItem(drawingStyle.getName());
      dsmi.addActionListener(
          e -> {
            final var gol = (GamePanel) inFrame.getContentPane();
            gol.setDrawing(false);
            gol.setDrawingStyle(drawingStyle);
          });

      if (!categories.containsKey(drawingStyle.getCategory())) {
        final var categoryMenu = new JMenu(drawingStyle.getCategory().getName());
        categories.put(drawingStyle.getCategory(), categoryMenu);
        drawingModeMenu.add(categoryMenu);
      }

      categories.get(drawingStyle.getCategory()).add(dsmi);
    }

    return menuBar;
  }

  private final JDesktopPane deskPane;

  private final InternalFrameAdapter internalFrameClosed =
      new InternalFrameAdapter() {
        @Override
        public void internalFrameClosed(final InternalFrameEvent e) {
          final var inFrame = e.getInternalFrame();
          final var gol = (GamePanel) inFrame.getContentPane();
          gol.dispose();
          MyMenuBar.this.deskPane.remove(inFrame);
        }
      };

  public MyMenuBar(final JDesktopPane deskPane) {
    final var newInstanceMenu = new JMenu("New Instance");
    this.add(newInstanceMenu);
    this.deskPane = deskPane;

    // calculate preferred sub-frame size
    final var maxWindowBoulds =
        GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds().getSize();
    final var maxWindowSide =
        (int) (Math.min(maxWindowBoulds.width, maxWindowBoulds.height) * 0.75);
    final var preferredFrameSize = new Dimension(maxWindowSide, maxWindowSide);

    // add menu items for different resolutions
    for (int i = 4; i < 14; ++i) {
      final var res = 1 << i;
      newInstanceMenu.add(this.makeInternalFrameCreatorMenuItem(deskPane, preferredFrameSize, res));
    }

    final var testMenuItem = new JMenuItem("Info");
    testMenuItem.addActionListener(
        e -> {
          final var inFrame = new InfoWindow();
          inFrame.setPreferredSize(preferredFrameSize);
          deskPane.add(inFrame);
        });
    newInstanceMenu.add(testMenuItem);
  }

  /** create a menu item to create a new internal frame */
  private JMenuItem makeInternalFrameCreatorMenuItem(
      final JDesktopPane deskPane, final Dimension preferredFrameSize, final int res) {
    final var menuItem = new JMenuItem(String.format("%dx%d", res, res));
    menuItem.addActionListener(
        e -> {
          // create a new internal frame
          final var inFrame =
              new JInternalFrame(
                  String.format("Game of Life %dx%d", res, res), true, true, true, true);
          inFrame.setPreferredSize(preferredFrameSize);

          inFrame.setJMenuBar(MyMenuBar.makeInternalFrameMenuBar(inFrame));
          final var gol = new GamePanel(res, res);
          inFrame.setContentPane(gol);
          deskPane.add(inFrame);
          inFrame.pack();
          inFrame.show();
          inFrame.addInternalFrameListener(this.internalFrameClosed);
        });
    return menuItem;
  }
}