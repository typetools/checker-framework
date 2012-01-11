import checkers.nullness.quals.*;

import java.util.*;
import java.io.*;

public class NonEmptyCollection {

  public static @NonNull String returnRemove(@NonNull PriorityQueue1<@NonNull String> pq) {
    return pq.remove();
  }

  public static @NonNull String returnPoll1(PriorityQueue1<@NonNull String> pq) {
    //:: error: (return.type.incompatible)
    return pq.poll();
  }

  public static @NonNull String returnPoll2(PriorityQueue1<@NonNull String> pq) {
    if (pq.isEmpty()) {
      return "hello";
    } else {
      return pq.poll();
    }
  }

  public static @NonNull String returnFirstKey(SortedMap<String, String> sm) {
    return sm.firstKey();
  }


  ///////////////////////////////////////////////////////////////////////////
  /// Helper classes copied from JDK
  ///

  public class PriorityQueue1<E> {
    public @Nullable E poll() { throw new RuntimeException("skeleton method"); }
    public E remove() { throw new RuntimeException("skeleton method"); }

    @AssertNonNullIfFalse("poll()")
    public boolean isEmpty() { throw new RuntimeException("skeleton method"); }
  }
}
