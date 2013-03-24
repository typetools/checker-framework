import checkers.nullness.quals.*;
import dataflow.quals.Pure;

public class AssertIfFalseTest2 {

  public static void usePriorityQueue(PriorityQueue1<@NonNull Object> active) {
    while (!(active.isEmpty())) {
      @NonNull Object queueMinPathNode = active.peek();
    }
  }

  ///////////////////////////////////////////////////////////////////////////
  /// Classes copied from the annotated JDK
  ///

  public class PriorityQueue1<E extends @NonNull Object> {
    @EnsuresNonNullIf(result=false, expression={"peek()"})
    @Pure public boolean isEmpty() { return true; }
    @Pure public @Nullable E peek() { return null; }
  }

}
