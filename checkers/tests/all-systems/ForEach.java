import java.util.*;
import checkers.nullness.quals.*;

public class ForEach {
  void m1() {
    Set<? extends CharSequence> s = new HashSet<CharSequence>( );
    for( CharSequence cs : s ) {
      cs.toString();
    }
  }

  void m2() {
    Set<CharSequence> s = new HashSet<CharSequence>( );
    for( CharSequence cs : s ) {
      cs.toString();
    }
  }

  <T extends @NonNull Object> void m3(T p) {
    Set<T> s = new HashSet<T>( );
    for( T cs : s ) {
      cs.toString();
    }
  }

  <T extends @NonNull Object> void m4(T p) {
    Set<T> s = new HashSet<T>( );
    for( Object cs : s ) {
      cs.toString();
    }
  }

  // An example taken from plume-lib's UtilMDE
  public static <T> List<T> removeDuplicates(List<T> l) {
    // There are shorter solutions that do not maintain order.
    HashSet<T> hs = new HashSet<T>(l.size());
    List<T> result = new ArrayList<T>();
    for (T t : l) {
      if (hs.add(t)) {
        result.add(t);
      }
    }
    return result;
  }

}
