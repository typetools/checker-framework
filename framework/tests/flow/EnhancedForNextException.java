import org.checkerframework.framework.testchecker.util.*;

// In an enhanced for loop over an Iterable, the call to next() can throw an exception.
public class EnhancedForNextException {
  void mayThrow() {}

  // The call to next() can throw an exception after an iteration has assigned p2 to l1.
  void nextThrows(@Odd String p1, String p2, Iterable<String> iterable) {
    String l1 = p1;
    try {
      mayThrow();
      for (String s : iterable) {
        l1 = p2;
      }
    } catch (RuntimeException e) {
      // :: error: [assignment]
      @Odd String l3 = l1;
    }
  }
}
