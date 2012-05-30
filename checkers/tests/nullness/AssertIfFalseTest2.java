import checkers.nullness.quals.*;

// @skip-test
public class AssertIfFalseTest2 {

  public static void usePriorityQueue(PriorityQueue1<@NonNull Object> active) {
    while (!(active.isEmpty())) {
      @NonNull Object queueMinPathNode = active.poll();
    }
  }

  ///////////////////////////////////////////////////////////////////////////
  /// Classes copied from the annotated JDK
  ///

  public class PriorityQueue1<E extends @NonNull Object> {
    @AssertNonNullIfFalse({"poll()", "peek()"})
    public boolean isEmpty() { throw new RuntimeException("skeleton method"); }
    public @Nullable E poll() { throw new RuntimeException("skeleton method"); }
    public @Nullable E peek() { throw new RuntimeException("skeleton method"); }
  }

}
