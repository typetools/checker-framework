import org.checkerframework.framework.testchecker.util.*;

// An enhanced for loop over a newly-created array cannot throw a NullPointerException.
public class EnhancedForNewArray {
  void mayThrow() {}

  void t1(@Odd String p1, String p2) {
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
}
