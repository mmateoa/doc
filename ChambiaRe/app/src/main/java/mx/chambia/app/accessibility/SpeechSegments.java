package mx.chambia.app.accessibility;

import java.util.ArrayList;
import java.util.List;

/** Lossless splitting below the engine limit, avoiding breaks within surrogate pairs. */
public final class SpeechSegments {
  private SpeechSegments() {}

  public static List<String> split(String text, int limit) {
    if (limit < 2) throw new IllegalArgumentException("Invalid speech limit");
    List<String> segments = new ArrayList<>();
    int start = 0;
    while (start < text.length()) {
      int end = Math.min(start + limit, text.length());
      if (end < text.length()) {
        if (Character.isHighSurrogate(text.charAt(end - 1))) end--;
        int boundary = text.lastIndexOf(' ', end - 1);
        if (boundary > start + limit / 2) end = boundary + 1;
      }
      segments.add(text.substring(start, end));
      start = end;
    }
    return segments;
  }
}
