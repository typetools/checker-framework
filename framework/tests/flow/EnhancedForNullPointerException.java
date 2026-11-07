import org.checkerframework.framework.testchecker.util.*;

// An enhanced for loop over an array throws a NullPointerException only if the array is null, and
// that happens before the first iteration.
public class EnhancedForNullPointerException {
  void mayThrow() {}

  // A newly-created array is not null.
  void newArray(@Odd String p1, String p2) {
    String l1 = p1;
    try {
      mayThrow();
      for (String s : new String[] {l1 = p2}) {
        l1 = p1;
      }
      l1 = p1;
    } catch (NullPointerException e) {
      @Odd String l3 = l1;
    }
  }

  // An array that may be null throws a NullPointerException before the first iteration.
  void mayBeNull(@Odd String p1, String p2, String[] array) {
    String l1 = p1;
    try {
      mayThrow();
      l1 = p2;
      for (String s : array) {
        l1 = p1;
      }
      l1 = p1;
    } catch (NullPointerException e) {
      // :: error: [assignment]
      @Odd String l3 = l1;
    }
  }

  // The loop does not throw a NullPointerException after its first iteration.
  void laterIterations(@Odd String p1, String p2, String[] array) {
    String l1 = p1;
    try {
      for (String s : array) {
        l1 = p2;
      }
      l1 = p1;
    } catch (NullPointerException e) {
      @Odd String l3 = l1;
    }
  }
}
