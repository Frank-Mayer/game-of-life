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

  private int capacity = 0;

  public IntSet(final int capacity) {
    this.set = new BitSet(capacity);
    this.capacity = capacity;
  }

  public void add(final int value) {
    if (value >= capacity) {
      this.resize(value);
    }
    this.set.set(value);
  }

  private void resize(final int newCapacity) {
    final BitSet newSet =
        new BitSet((int) Math.pow(2, Math.ceil(Math.log(newCapacity) / Math.log(2))));
    newSet.or(set);
    this.set = newSet;
    this.capacity = newCapacity;
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

  public int size() {
    return this.set.cardinality();
  }

  @Override
  public Iterator<Integer> iterator() {
    return new Iter();
  }

  // public void addAll(final Iterable<? extends Integer> c) {
  //   for (final Integer i : c) {
  //     this.add(i);
  //   }
  // }

  public void addAll(final IntSet s) {
    if (s.capacity > this.capacity) {
      this.resize(s.capacity);
    }
    this.set.or(s.set);
  }
}