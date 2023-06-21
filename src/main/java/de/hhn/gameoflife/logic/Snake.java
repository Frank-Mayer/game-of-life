package de.hhn.gameoflife.logic;

import de.hhn.gameoflife.control_iface.Disposable;
import de.hhn.gameoflife.control_iface.Drawable;
import de.hhn.gameoflife.data_structures.IntSet;
import de.hhn.gameoflife.data_structures.RingBuffer;
import de.hhn.gameoflife.ui.Alert;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class Snake implements Disposable {
  public static class SnakeKeyListener extends KeyAdapter {
    @Override
    public void keyPressed(final KeyEvent e) {
      for (final var snake : Snake.snakes) {
        switch (e.getKeyCode()) {
          case KeyEvent.VK_UP, KeyEvent.VK_K -> snake.setDirection(Direction.UP);
          case KeyEvent.VK_DOWN, KeyEvent.VK_J -> snake.setDirection(Direction.DOWN);
          case KeyEvent.VK_LEFT, KeyEvent.VK_H -> snake.setDirection(Direction.LEFT);
          case KeyEvent.VK_RIGHT, KeyEvent.VK_L -> snake.setDirection(Direction.RIGHT);
        }
      }
    }
  }

  public static final Set<Snake> snakes = new HashSet<>();
  private final RingBuffer<Integer> positions;
  private Direction direction;
  private boolean active = true;
  private final int worldWidth;
  private final int worldHeight;
  private Consumer<Iterable<Integer>> onChangeConsumer;
  private final ScheduledExecutorService sheduler;
  private boolean disposed = false;
  private final World world;
  private final Drawable<IntSet> worldUI;

  public Snake(final Settings settings, final World world, final Drawable<IntSet> worldUI) {
    this.worldWidth = settings.worldWidth();
    this.worldHeight = settings.worldHeight();
    this.world = world;
    this.worldUI = worldUI;
    this.positions = new RingBuffer<>(4);
    this.reset();
    Snake.snakes.add(this);
    this.sheduler = Executors.newScheduledThreadPool(1);
    this.sheduler.scheduleWithFixedDelay(this::tick, 400, 400, TimeUnit.MILLISECONDS);
  }

  public void reset() {
    this.positions.clear();
    this.positions.add(0);
    this.direction = Direction.RIGHT;
    this.active = false;
  }

  public void start() {
    this.active = true;
  }

  public void tick() {
    if (!this.active) {
      return;
    }
    for (final var el : this.positions) {
      if (this.world.getWorldData().contains(el)) {
        Alert.show("Game over", "You were eaten up by the evil cells", null);
        this.positions.clear();
        this.active = false;
        this.onChangeConsumer.accept(this.positions);
        return;
      }
    }
    final var head = this.positions.getHead();
    var x = head % this.worldWidth;
    var y = head / this.worldWidth;
    switch (this.direction) {
      case UP -> --y;
      case DOWN -> ++y;
      case LEFT -> --x;
      case RIGHT -> ++x;
    }
    if (x < 0) {
      x = this.worldWidth - 1;
    } else if (x >= this.worldWidth) {
      x = 0;
    } else if (y < 0) {
      y = this.worldHeight - 1;
    } else if (y >= this.worldHeight) {
      y = 0;
    }
    final var newHead = y * this.worldWidth + x;
    if (this.positions.contains(newHead)) {
      // bite yourself
      this.active = false;
      Alert.show("Game over", "You bit yourself!", null);
      this.active = false;
      this.positions.clear();
    } else {
      this.positions.add(newHead);
    }
    this.onChangeConsumer.accept(this.positions);
  }

  public void onChange(final Consumer<Iterable<Integer>> consumer) {
    this.onChangeConsumer = consumer;
    consumer.accept(this.positions);
  }

  public void setDirection(final Direction direction) {
    this.direction = direction;
  }

  @Override
  public void dispose() {
    if (this.disposed) {
      return;
    }
    this.disposed = true;
    this.sheduler.shutdown();
    Snake.snakes.remove(this);
  }
}