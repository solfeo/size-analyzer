package com.android.tools.sizereduction.plugin;

/** Utility class to convert numbers into proper display formats. */
public class DisplayUtil {
  private static final String[] UNITS = {"B", "KB", "MB", "GB", "TB"};

  public static String renderBytesSaved(Long bytesSaved) {
    if (bytesSaved != null && bytesSaved > 0) {
      int unit = (int) (Math.log(bytesSaved) / Math.log(1024));
      if (unit == 0) {
        return String.format("%.0f ", bytesSaved / Math.pow(1024, unit)) + UNITS[unit];
      }
      return String.format("%.2f ", bytesSaved / Math.pow(1024, unit)) + UNITS[unit];
    }
    return null;
  }
}
