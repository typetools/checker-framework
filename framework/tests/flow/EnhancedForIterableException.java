import org.checkerframework.framework.testchecker.util.*;

// An enhanced for loop over an Iterable desugars to calls to hasNext() and next(), which can throw
// exceptions after an iteration of the loop body.
public class EnhancedForIterableException {

  void laterIterations(@Odd String p1, String p2, Iterable<String> it) {
    String l1 = p1;
    try {
      for (String s : it) {
        l1 = p2;
      }
      l1 = p1;
    } catch (RuntimeException e) {
      // :: error: [assignment]
      @Odd String l3 = l1;
    }
  }

  // The CFG represents a possibly-null receiver of hasNext() by a NullPointerException edge, but
  // only an invocation has an Error edge.
  void laterIterationsError(@Odd String p1, String p2, Iterable<String> it) {
    String l1 = p1;
    try {
      for (String s : it) {
        l1 = p2;
      }
      l1 = p1;
    } catch (Error e) {
      // :: error: [assignment]
      @Odd String l3 = l1;
    }
  }
}
