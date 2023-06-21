package de.hhn.gameoflife.logic;

import de.hhn.gameoflife.data_structures.RingBuffer;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class Snake {
  public static class SnakeKeyListener extends KeyAdapter {
    @Override
    public void keyPressed(final KeyEvent e) {
      System.out.println("key pressed");
      for (final var snake : Snake.snakes) {
        switch (e.getKeyCode()) {
          case KeyEvent.VK_UP:
          case KeyEvent.VK_K:
            snake.setDirection(Direction.UP);
            break;
          case KeyEvent.VK_DOWN:
          case KeyEvent.VK_J:
            snake.setDirection(Direction.DOWN);
            break;
          case KeyEvent.VK_LEFT:
          case KeyEvent.VK_H:
            snake.setDirection(Direction.LEFT);
            break;
          case KeyEvent.VK_RIGHT:
          case KeyEvent.VK_L:
            snake.setDirection(Direction.RIGHT);
            break;
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

  public Snake(final Settings settings) {
    this.worldWidth = settings.worldWidth();
    this.worldHeight = settings.worldHeight();
    this.positions = new RingBuffer<>(4);
    this.positions.add(0);
    this.direction = Direction.RIGHT;
    this.active = false;
    Snake.snakes.add(this);
  }

  public void start() {
    this.active = true;
  }

  public void tick() {
    if (!this.active) {
      return;
    }
    final var head = this.positions.getHead();
    var x = head % this.worldWidth;
    var y = head / this.worldWidth;
    switch (this.direction) {
      case UP:
        --y;
        break;
      case DOWN:
        ++y;
        break;
      case LEFT:
        --x;
        break;
      case RIGHT:
        ++x;
        break;
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
      System.out.println("Game over!");
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
}