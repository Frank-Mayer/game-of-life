package de.hhn.gameoflife.data_structures;

import java.util.Iterator;

public class RingBuffer<T> implements Iterable<T> {
  public static void main(String[] args) {
    final var rb = new RingBuffer<Integer>(4);
    for(int i = 1; i != 7; ++i) {
      rb.add(i);
      for(final var x : rb) {
        System.out.print(x);
      }
      System.out.println();
    }
  }
  private class RingBufferIterator implements Iterator<T> {
    private int current;
    private final int length;

    public RingBufferIterator() {
      this.current = -1;
      this.length = RingBuffer.this.buffer.length;
    }

    @Override
    public boolean hasNext() {
      ++this.current;
      while (this.current != this.length) {
        if (RingBuffer.this.buffer[this.current] != null) {
          return true;
        }
        ++this.current;
      }
      return false;
    }

    @Override
    public T next() {
      return RingBuffer.this.buffer[this.current];
    }
  }
  private final T[] buffer;
  private final int maxSize;
  private int head;

  private int tail;

  public RingBuffer(final int maxSize) {
    this.maxSize = maxSize;
    this.buffer = (T[]) new Object[maxSize];
    this.head = -1;
    this.tail = 0;
  }

  public void grow(final int add) {}

  public void add(final T element) {
    if (++this.head == this.maxSize) {
      this.head = 0;
      this.tail = 1;
    } else if (this.tail != 0) {
      ++this.tail;
    }
    this.buffer[this.head] = element;
  }

  @Override
  public Iterator<T> iterator() {
    return new RingBufferIterator();
  }

  public T getHead() {
    return this.buffer[this.head];
  }

  public boolean contains(final T element) {
    for (final var el : this) {
      if (el.equals(element)) {
        return true;
      }
    }
    return false;
  }
}