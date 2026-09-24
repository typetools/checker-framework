import java.util.Deque;
import java.util.Map;
import org.checkerframework.checker.modifiability.qual.Growable;
import org.checkerframework.checker.modifiability.qual.MaybeGrowable;
import org.checkerframework.checker.modifiability.qual.MaybeShrinkable;
import org.checkerframework.checker.modifiability.qual.Modifiable;
import org.checkerframework.checker.modifiability.qual.Replaceable;
import org.checkerframework.checker.modifiability.qual.SeqGrowable;
import org.checkerframework.checker.modifiability.qual.SeqUngrowable;
import org.checkerframework.checker.modifiability.qual.Unmodifiable;

/**
 * A type variable is classified by its upper bound, so an alias written on it expands exactly as it
 * does on the bound.
 */
public class GenericCapabilityTest {

  <T extends Deque<String>> void modifiableTypeVariable(@Modifiable T deque) {
    @SeqGrowable T seqGrowable = deque;
  }

  <T extends Deque<String>> void unmodifiableTypeVariable(@Unmodifiable T deque) {
    @SeqUngrowable T seqUngrowable = deque;
  }

  <T extends Map.Entry<String, String>> void entryTypeVariable(@Modifiable T entry) {
    // `Map.Entry` has no grow or shrink methods, so `@Modifiable` claims only replacement.
    @MaybeGrowable @MaybeShrinkable @Replaceable T canonical = entry;
    // :: error: [assignment]
    @Growable T growable = entry;
  }
}
