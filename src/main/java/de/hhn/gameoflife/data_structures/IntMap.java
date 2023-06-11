package de.hhn.gameoflife.data_structures;

import java.util.Arrays;

public class IntMap {

  private int[] map;

  private int capacity = 0;

  public IntMap(final int capacity) {
    this.map = new int[capacity];
    this.capacity = capacity;
  }

  public void put(final int key, final int value) {
    if (key >= capacity) {
      this.resize(key);
    }
    this.map[key] = value;
  }

  public void remove(final int key) {
    this.map[key] = 0;
  }

  public int get(final int key) {
    return this.map[key];
  }

  public void clear() {
    Arrays.fill(map, 0);
  }

  public boolean containsKey(final int key) {
    if (key < 0 || key >= this.capacity) {
      return false;
    }
    return this.map[key] != 0;
  }

  public void increment(final int index) {
    if (index >= this.capacity) {
      this.resize(index);
    }
    ++this.map[index];
  }

  private void resize(final int newCapacity) {
    final int[] newMap = new int[(int) Math.pow(2, Math.ceil(Math.log(newCapacity) / Math.log(2)))];
    System.arraycopy(map, 0, newMap, 0, map.length);
    this.map = newMap;
    this.capacity = newCapacity;
  }
}