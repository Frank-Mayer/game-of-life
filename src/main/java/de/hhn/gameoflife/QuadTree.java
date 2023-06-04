package de.hhn.gameoflife;

import java.util.Random;

public class QuadTree {
  private final QuadTree ne;
  private final QuadTree nw;
  private final QuadTree se;
  private final QuadTree sw;
  private final State state;

  private final static Random random = new Random();

  public QuadTree(final int size) {
    if (size == 1) {
      this.ne = null;
      this.nw = null;
      this.se = null;
      this.sw = null;
      this.state = State.random();
    } else {
      this.ne = new QuadTree(size / 2);
      this.nw = new QuadTree(size / 2);
      this.se = new QuadTree(size / 2);
      this.sw = new QuadTree(size / 2);
      this.state = State.NOT_LEAF;
    }
  }

  @Override
  public int hashCode() {
    switch (this.state) {
      case ALIVE:
        return 1;
      case DEAD:
        return 0;
      case NOT_LEAF:
        return this.ne.hashCode() + this.nw.hashCode() + this.se.hashCode() + this.sw.hashCode();
      default:
        throw new IllegalStateException();
    }
  }

  private static enum State {
    ALIVE,
    DEAD,
    NOT_LEAF;

    public static State random() {
      return State.values()[QuadTree.random.nextInt(State.values().length)];
    }
  }
}