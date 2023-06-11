package de.hhn.gameoflife.util;

import java.awt.image.BufferedImage;

/** Utility class for dithering images. */
public class Dithering {
  /**
   * Creates a new image that is a grayscale version of the given image.
   *
   * <p>This method assumes that the image is in the RGB color space.
   *
   * <p>The returned image is of type {@link BufferedImage#TYPE_BYTE_GRAY}.
   *
   * <p>The returned image is a new image, the given image is not modified.
   *
   * <p>The returned image is not dithered.
   *
   * @param image An image to be grayscaled.
   */
  public static void grayScale(final BufferedImage image) {
    if (image.getType() == BufferedImage.TYPE_BYTE_GRAY) {
      return;
    }
    final int width = image.getWidth();
    final int height = image.getHeight();
    final int[] pixels = image.getRGB(0, 0, width, height, null, 0, width);

    for (int i = 0; i < pixels.length; i++) {
      final int pixel = pixels[i] & 0x00FFFFFF; // remove alpha channel from getRGB()
      final int r = (pixel >> 16) & 0xFF;
      final int g = (pixel >> 8) & 0xFF;
      final int b = pixel & 0xFF;
      final int gray = (r + g + b) / 3;
      pixels[i] =
          (pixels[i] & 0xFF000000)
              | (gray << 16)
              | (gray << 8)
              | gray; // add alpha channel for setRGB()
    }

    image.setRGB(0, 0, width, height, pixels, 0, width);
  }

  /**
   * Modifies a given image by applying the Floyd-Steinberg error diffusion dithering algorithm.
   *
   * <p>This method assumes that the image is already in grayscale and TYPE_BYTE_GRAY.
   *
   * @param image A grayscale image to be modified.
   */
  public static void floydSteinberg(final BufferedImage image) {
    final var width = image.getWidth();
    final var height = image.getHeight();
    final var pixels = image.getRGB(0, 0, width, height, null, 0, width);
    for (var x = 0; x < width; ++x) {
      for (var y = 0; y < height; ++y) {
        final int i = y * width + x;
        final int oldGray = pixels[i] & 0xFF;
        final int newGray = oldGray < 128 ? 0 : 255;
        pixels[i] = (pixels[i] & 0xFF000000) | (newGray << 16) | (newGray << 8) | newGray;
        final int error = oldGray - newGray;
        if (x + 1 < width) {
          final int oldGray2 = pixels[i + 1] & 0xFF;
          final int newGray2 = Dithering.clamp(oldGray2 + (error * 7) / 16);
          pixels[i + 1] =
              (pixels[i + 1] & 0xFF000000) | (newGray2 << 16) | (newGray2 << 8) | newGray2;
        }
        if (x > 0 && y + 1 < height) {
          final int oldGray3 = pixels[i + width - 1] & 0xFF;
          final int newGray3 = Dithering.clamp(oldGray3 + (error * 3) / 16);
          pixels[i + width - 1] =
              (pixels[i + width - 1] & 0xFF000000) | (newGray3 << 16) | (newGray3 << 8) | newGray3;
        }
        if (y + 1 < height) {
          final int oldGray4 = pixels[i + width] & 0xFF;
          final int newGray4 = Dithering.clamp(oldGray4 + (error * 5) / 16);
          pixels[i + width] =
              (pixels[i + width] & 0xFF000000) | (newGray4 << 16) | (newGray4 << 8) | newGray4;
        }
        if (x + 1 < width && y + 1 < height) {
          final int oldGray5 = pixels[i + width + 1] & 0xFF;
          final int newGray5 = Dithering.clamp(oldGray5 + (error * 1) / 16);
          pixels[i + width + 1] =
              (pixels[i + width + 1] & 0xFF000000) | (newGray5 << 16) | (newGray5 << 8) | newGray5;
        }
      }
    }
    image.setRGB(0, 0, width, height, pixels, 0, width);
  }

  /** Clamp a given integer to the range [0, 255]. */
  private static int clamp(final int i) {
    return Dithering.clamp(0, i, 0xff);
  }

  /** Clamp a given integer to the range [min, max]. */
  private static int clamp(final int min, final int i, final int max) {
    return Math.max(min, Math.min(i, max));
  }

  private Dithering() {}
}