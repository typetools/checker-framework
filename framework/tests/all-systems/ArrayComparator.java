// Caused a crash in WPI.

import java.util.*;

class ArrayComparator {

  private final SortedMap<byte[], byte[]> map = new TreeMap<>();

  public Comparator<? super byte[]> comparator() {
    return map.comparator();
  }
}
