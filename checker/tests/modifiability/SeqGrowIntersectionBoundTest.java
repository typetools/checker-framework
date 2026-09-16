// Tests the expansion of `@Modifiable` on a type variable whose upper bound is an intersection
// type.  A value of an intersection type is a value of each of its bounds, so it has a capability
// if any bound does.

import java.util.Deque;
import java.util.Set;
import org.checkerframework.checker.modifiability.qual.MaybeSeqGrowable;
import org.checkerframework.checker.modifiability.qual.Modifiable;
import org.checkerframework.checker.modifiability.qual.SeqGrowable;

public class SeqGrowIntersectionBoundTest {

  // `Deque` has sequenced-grow methods, so `@Modifiable` expands to `@SeqGrowable`.
  <T extends Deque<String> & Cloneable> void intersectionBoundHasCapability(@Modifiable T deque) {
    @SeqGrowable T s = deque;
  }

  // No bound has sequenced-grow methods, so `@Modifiable` expands to `@MaybeSeqGrowable`.
  <T extends Set<String> & Cloneable> void intersectionBoundLacksCapability(@Modifiable T set) {
    @MaybeSeqGrowable T m = set;
  }
}
