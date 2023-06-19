package de.hhn.gameoflife.logic;

import de.hhn.gameoflife.data_structures.RingBuffer;
import java.util.function.Consumer;

public class Snake {
  private final RingBuffer<Integer> positions;
  private final Direction direction;
  private boolean active;
  private final int worldWidth;
  private final int worldHeight;
  private Consumer<Iterable<Integer>> onChangeConsumer;

  public Snake(final Settings settings) {
    this.worldWidth = settings.worldWidth();
    this.worldHeight = settings.worldHeight();
    this.positions = new RingBuffer<>(4);
    this.positions.add(0);
    this.positions.add(1);
    this.positions.add(2);
    this.positions.add(3);
    this.direction = Direction.RIGHT;
    this.active = false;
  }

  public void tick() {
    if (this.active) {
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
  }

  public void onChange(final Consumer<Iterable<Integer>> consumer) {
    this.onChangeConsumer = consumer;
    consumer.accept(this.positions);
  }
}