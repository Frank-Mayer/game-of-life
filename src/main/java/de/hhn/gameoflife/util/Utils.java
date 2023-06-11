package de.hhn.gameoflife.util;

public class Utils {
  public static int log2(final Number x) {
    return (int) Math.ceil(Math.log(x.doubleValue()) / Math.log(2));
  }

  private Utils() {}
}