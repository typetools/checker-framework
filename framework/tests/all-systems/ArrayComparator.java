// Caused a crash in WPI.

import java.util.*;

@SuppressWarnings("allcheckers") // Only check for crashes
class ArrayComparator {

  private final SortedMap<byte[], byte[]> map = new TreeMap<>();

  public Comparator<? super byte[]> comparator() {
    return map.comparator();
  }
}
