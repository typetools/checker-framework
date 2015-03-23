import java.util.*;

public final class MissingBoundAnnotations {
  @SuppressWarnings({"nullness:type.argument.type.incompatible", "javari:type.argument.type.incompatible"})
  public static <K extends Comparable<? super K>,V> Collection<K> sortedKeySet(Map<K,V> m) {
    ArrayList<K> theKeys = new ArrayList<K>(m.keySet());
    Collections.sort(theKeys);
    return theKeys;
  }
}

