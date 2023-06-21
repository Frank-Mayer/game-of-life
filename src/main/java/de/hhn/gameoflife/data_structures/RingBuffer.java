package de.hhn.gameoflife.data_structures;

import java.util.Arrays;
import java.util.Iterator;
import static de.hhn.gameoflife.util.State.useState;

public class RingBuffer<T> implements Iterable<T> {
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

  private T[] buffer;
  private int maxSize;
  private int head;

  private int tail;

  public RingBuffer(final int maxSize) {
    this.maxSize = maxSize;
    this.buffer = (T[]) new Object[maxSize];
    this.head = -1;
    this.tail = 0;
  }

  public synchronized void grow(final int add) {
    final var newMaxSize = this.maxSize + add;
    final var newBuffer = (T[]) new Object[newMaxSize];
    final var i = useState(0);
    Arrays.stream(this.buffer).sorted().forEach(el -> {
      newBuffer[i.get()] = el;
      i.set(i.get() + 1);
    });
    this.buffer = newBuffer;
    this.maxSize = newMaxSize;
    this.head = i.get() - 1;
    this.tail = 0;
  }

  public synchronized void add(final T element) {
    if ((++this.head) == this.maxSize) {
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

  public void clear() {
    for (int i = 0; i < this.buffer.length; ++i) {
      this.buffer[i] = null;
    }
    this.head = -1;
    this.tail = 0;
  }
}