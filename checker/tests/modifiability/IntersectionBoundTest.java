// Tests the expansion of `@Modifiable` on a type variable whose upper bound is an intersection
// type.  A value of an intersection type is a value of each of its bounds, so it has a capability
// if any bound does.

import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.checkerframework.checker.modifiability.qual.Growable;
import org.checkerframework.checker.modifiability.qual.MaybeSeqGrowable;
import org.checkerframework.checker.modifiability.qual.Modifiable;
import org.checkerframework.checker.modifiability.qual.SeqGrowable;

public class IntersectionBoundTest {

  // `Deque` has sequenced-grow methods, so `@Modifiable` expands to `@SeqGrowable`.
  <T extends Deque<String> & Cloneable> void intersectionBoundHasCapability(@Modifiable T deque) {
    @SeqGrowable T s = deque;
  }

  // No bound has sequenced-grow methods, so `@Modifiable` expands to `@MaybeSeqGrowable`.
  <T extends Set<String> & Cloneable> void intersectionBoundLacksCapability(@Modifiable T set) {
    @MaybeSeqGrowable T m = set;
  }

  // `List` has grow methods, so `@Modifiable` expands to `@Growable`, even though the other bound
  // is not a collection at all.
  <T extends List<String> & Cloneable> void growIntersectionBoundHasCapability(@Modifiable T list) {
    @Growable T g = list;
  }

  // Neither `Map.Entry` nor a plain `Iterator` has grow methods, so `@Modifiable` expands to
  // `@MaybeGrowable`, which does not satisfy `@Growable`.
  <T extends Map.Entry<String, String> & Iterator<String>>
      void growIntersectionBoundLacksCapability(@Modifiable T entry) {
    // :: error: [assignment]
    @Growable T g = entry;
  }
}
