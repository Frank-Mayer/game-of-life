package de.hhn.gameoflife.data_structures;

import java.util.Iterator;

public class RingBuffer<T> implements Iterable<T> {
  private T[] buffer;
  private int maxSize;
  private int head;
  private int tail;

  public RingBuffer(int maxSize) {
    this.maxSize = maxSize;
    this.head = -1;
    this.tail = -1;
    this.buffer = (T[]) new Object[maxSize];
  }

  public void grow(final int add) {
    this.maxSize += add;
    final var newBuffer = (T[]) new Object[this.maxSize];
    for (int i = this.tail, j = 0; i != this.head; i = (i + 1) % this.maxSize, ++j) {
      newBuffer[j] = this.buffer[i];
    }
    this.buffer = newBuffer;
    this.head = this.maxSize - 1;
    this.tail = 0;
  }

  public void add(T element) {
    ++this.head;
    if (this.head == this.maxSize) {
      this.head = 0;
      this.tail = 1;
    } else if (this.head == this.tail) {
      ++this.tail;
      if (this.tail == this.maxSize) {
        this.tail = 0;
      }
    } else {
      this.tail = this.head + 1;
    }
    this.buffer[this.head] = element;
  }

  @Override
  public Iterator<T> iterator() {
    return new Iterator<T>() {
      private int current = RingBuffer.this.tail;

      @Override
      public boolean hasNext() {
        return this.current != RingBuffer.this.head;
      }

      @Override
      public T next() {
        ++this.current;
        if (this.current == RingBuffer.this.maxSize) {
          this.current = 0;
        }
        return RingBuffer.this.buffer[this.current];
      }
    };
  }

  public T getHead() {
    return this.buffer[this.head];
  }

  public boolean contains(T element) {
    for (final var el : this) {
      if (el.equals(element)) {
        return true;
      }
    }
    return false;
  }
}