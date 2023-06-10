package de.hhn.gameoflife;

import java.util.BitSet;
import java.util.Iterator;

public class IntSet implements Iterable<Integer> {
  private class Iter implements Iterator<Integer> {
    private int next = 0;

    @Override
    public boolean hasNext() {
      return set.nextSetBit(next) != -1;
    }

    @Override
    public Integer next() {
      next = set.nextSetBit(next);
      return next++;
    }

    @Override
    public void remove() {
      set.clear(next);
    }
  }

  private BitSet set;

  public IntSet(final int capacity) {
    this.set = new BitSet(capacity);
  }

  public void add(final int value) {
    this.set.set(value);
  }

  public void remove(final int value) {
    this.set.clear(value);
  }

  public void clear() {
    this.set.clear();
  }

  public boolean contains(final int value) {
    return this.set.get(value);
  }

  @Override
  public Iterator<Integer> iterator() {
    return new Iter();
  }

  public void overwrite(IntSet in) {
    this.set = in.set;
  }
}