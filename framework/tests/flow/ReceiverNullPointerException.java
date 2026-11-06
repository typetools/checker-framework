import org.checkerframework.framework.testchecker.util.*;

// Accessing a member of a receiver that is never null does not throw a NullPointerException.
//
// In each method that calls equals(), `l1` is p2 only between `l1 = p2` and the argument `l1 = p1`,
// where the access of `equals` is the only node that could throw a NullPointerException.
public class ReceiverNullPointerException extends ReceiverNullPointerExceptionSuper {
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

  void arrayAccess(@Odd String p1, String p2) {
    String l1 = p1;
    try {
      mayThrow();
      l1 = p2;
      int n = new int[] {1}[0];
      l1 = p1;
    } catch (NullPointerException e) {
      @Odd String l3 = l1;
    }
  }

  void assignment(@Odd String p1, String p2) {
    String l1 = p1;
    int[] a;
    try {
      mayThrow();
      l1 = p2;
      int n = (a = new int[1]).length;
      l1 = p1;
    } catch (NullPointerException e) {
      @Odd String l3 = l1;
    }
  }

  void superField(@Odd String p1, String p2) {
    String l1 = p1;
    try {
      mayThrow();
      l1 = p2;
      Object o = super.f;
      l1 = p1;
    } catch (NullPointerException e) {
      @Odd String l3 = l1;
    }
  }

  void fieldAssignment(@Odd String p1, String p2) {
    String l1 = p1;
    try {
      mayThrow();
      l1 = p2;
      ((ReceiverNullPointerExceptionSuper) this).f = p2;
      l1 = p1;
    } catch (NullPointerException e) {
      @Odd String l3 = l1;
    }
  }

  void stringLiteral(@Odd String p1, String p2) {
    String l1 = p1;
    try {
      mayThrow();
      l1 = p2;
      "abc".equals(l1 = p1);
    } catch (NullPointerException e) {
      @Odd String l3 = l1;
    }
  }

  void stringConcatenation(@Odd String p1, String p2) {
    String l1 = p1;
    try {
      mayThrow();
      l1 = p2;
      (p2 + p2).equals(l1 = p1);
    } catch (NullPointerException e) {
      @Odd String l3 = l1;
    }
  }

  void superCall(@Odd String p1, String p2) {
    String l1 = p1;
    try {
      mayThrow();
      l1 = p2;
      super.equals(l1 = p1);
    } catch (NullPointerException e) {
      @Odd String l3 = l1;
    }
  }

  void lambda(@Odd String p1, String p2) {
    String l1 = p1;
    try {
      mayThrow();
      l1 = p2;
      ((Runnable) () -> {}).equals(l1 = p1);
    } catch (NullPointerException e) {
      @Odd String l3 = l1;
    }
  }

  void methodReference(@Odd String p1, String p2) {
    String l1 = p1;
    try {
      mayThrow();
      l1 = p2;
      ((Runnable) this::mayThrow).equals(l1 = p1);
    } catch (NullPointerException e) {
      @Odd String l3 = l1;
    }
  }

  void classLiteral(@Odd String p1, String p2) {
    String l1 = p1;
    try {
      mayThrow();
      l1 = p2;
      String.class.equals(l1 = p1);
    } catch (NullPointerException e) {
      @Odd String l3 = l1;
    }
  }

  void switchExpression(@Odd String p1, String p2, int i) {
    String l1 = p1;
    try {
      mayThrow();
      l1 = p2;
      (switch (i) {
            case 1 -> "a";
            default -> "b";
          })
          .equals(l1 = p1);
    } catch (NullPointerException e) {
      @Odd String l3 = l1;
    }
  }

  void switchExpressionMayBeNull(@Odd String p1, String p2, int i, String s) {
    String l1 = p1;
    try {
      mayThrow();
      l1 = p2;
      (switch (i) {
            case 1 -> "a";
            default -> s;
          })
          .equals(l1 = p1);
    } catch (NullPointerException e) {
      // :: error: [assignment]
      @Odd String l3 = l1;
    }
  }

  void methodCallMayBeNull(@Odd String p1, String p2, String s) {
    String l1 = p1;
    try {
      mayThrow();
      l1 = p2;
      s.equals(l1 = p1);
    } catch (NullPointerException e) {
      // :: error: [assignment]
      @Odd String l3 = l1;
    }
  }

  class Inner {
    void qualifiedThis(@Odd String p1, String p2) {
      String l1 = p1;
      try {
        mayThrow();
        l1 = p2;
        ReceiverNullPointerException.this.equals(l1 = p1);
      } catch (NullPointerException e) {
        @Odd String l3 = l1;
      }
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

class ReceiverNullPointerExceptionSuper {
  Object f;
}
