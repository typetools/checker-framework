import org.checkerframework.framework.testchecker.util.*;

// Accessing a member of a receiver that is never null does not throw a NullPointerException.
public class ReceiverNullPointerException {
  void mayThrow() {}

  void newArray(@Odd String p1, String p2) {
    String l1 = p1;
    try {
      mayThrow();
      int n = new String[] {l1 = p2}.length;
      l1 = p1;
    } catch (NullPointerException e) {
      @Odd String l3 = l1;
    }
  }

  void cast(@Odd String p1, String p2) {
    String l1 = p1;
    try {
      mayThrow();
      int n = ((Object[]) new String[] {l1 = p2}).length;
      l1 = p1;
    } catch (NullPointerException e) {
      @Odd String l3 = l1;
    }
  }

  void ternary(@Odd String p1, String p2, boolean b) {
    String l1 = p1;
    try {
      mayThrow();
      int n = (b ? new String[] {l1 = p2} : new String[0]).length;
      l1 = p1;
    } catch (NullPointerException e) {
      @Odd String l3 = l1;
    }
  }

  void enhancedForCast(@Odd String p1, String p2) {
    String l1 = p1;
    try {
      mayThrow();
      for (String s : (String[]) new String[] {l1 = p2}) {
        l1 = p1;
      }
      l1 = p1;
    } catch (NullPointerException e) {
      @Odd String l3 = l1;
    }
  }

  void mayBeNull(@Odd String p1, String p2, String[] a) {
    String l1 = p1;
    try {
      mayThrow();
      l1 = p2;
      int n = a.length;
      l1 = p1;
    } catch (NullPointerException e) {
      // :: error: [assignment]
      @Odd String l3 = l1;
    }
  }
}
