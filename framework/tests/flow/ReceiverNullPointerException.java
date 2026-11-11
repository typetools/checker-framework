import org.checkerframework.framework.testchecker.util.*;

// Accessing a member of a receiver that is never null does not throw a NullPointerException.
//
// In each method that creates a bound method reference, `l1` is p2 only between `l1 = p2` and
// `l1 = p1`, where creating the method reference is the only node that could throw a
// NullPointerException.
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
      Runnable r = "abc"::length;
      l1 = p1;
    } catch (NullPointerException e) {
      @Odd String l3 = l1;
    }
  }

  void stringConcatenation(@Odd String p1, String p2) {
    String l1 = p1;
    try {
      mayThrow();
      l1 = p2;
      Runnable r = (p2 + p2)::length;
      l1 = p1;
    } catch (NullPointerException e) {
      @Odd String l3 = l1;
    }
  }

  void superMethodReference(@Odd String p1, String p2) {
    String l1 = p1;
    try {
      mayThrow();
      l1 = p2;
      Runnable r = super::hashCode;
      l1 = p1;
    } catch (NullPointerException e) {
      @Odd String l3 = l1;
    }
  }

  void lambda(@Odd String p1, String p2) {
    String l1 = p1;
    try {
      mayThrow();
      l1 = p2;
      Runnable r = ((Runnable) () -> {})::run;
      l1 = p1;
    } catch (NullPointerException e) {
      @Odd String l3 = l1;
    }
  }

  void methodReference(@Odd String p1, String p2) {
    String l1 = p1;
    try {
      mayThrow();
      l1 = p2;
      Runnable r = ((Runnable) this::mayThrow)::run;
      l1 = p1;
    } catch (NullPointerException e) {
      @Odd String l3 = l1;
    }
  }

  void boundMethodReferenceMayBeNull(@Odd String p1, String p2, Runnable r) {
    String l1 = p1;
    try {
      mayThrow();
      l1 = p2;
      ((Runnable) r::run).equals(l1 = p1);
    } catch (NullPointerException e) {
      // :: error: [assignment]
      @Odd String l3 = l1;
    }
  }

  void unboundMethodReference(@Odd String p1, String p2) {
    String l1 = p1;
    try {
      mayThrow();
      l1 = p2;
      ((java.util.function.Function<Object, String>) Object::toString).equals(l1 = p1);
    } catch (NullPointerException e) {
      @Odd String l3 = l1;
    }
  }

  void classLiteral(@Odd String p1, String p2) {
    String l1 = p1;
    try {
      mayThrow();
      l1 = p2;
      Runnable r = String.class::getName;
      l1 = p1;
    } catch (NullPointerException e) {
      @Odd String l3 = l1;
    }
  }

  enum Color {
    RED
  }

  void enumConstant(@Odd String p1, String p2) {
    String l1 = p1;
    try {
      mayThrow();
      l1 = p2;
      Runnable r = Color.RED::name;
      l1 = p1;
    } catch (NullPointerException e) {
      // An enum constant is null if it is read during class initialization before it is assigned.
      // :: error: [assignment]
      @Odd String l3 = l1;
    }
  }

  static Color favoriteColor;

  void enumFieldMayBeNull(@Odd String p1, String p2) {
    String l1 = p1;
    try {
      mayThrow();
      l1 = p2;
      Runnable r = favoriteColor::name;
      l1 = p1;
    } catch (NullPointerException e) {
      // :: error: [assignment]
      @Odd String l3 = l1;
    }
  }

  void switchExpression(@Odd String p1, String p2, int i) {
    String l1 = p1;
    try {
      mayThrow();
      l1 = p2;
      Runnable r =
          (switch (i) {
                case 1 -> "a";
                default -> "b";
              })
              ::length;
      l1 = p1;
    } catch (NullPointerException e) {
      @Odd String l3 = l1;
    }
  }

  void switchExpressionMayBeNull(@Odd String p1, String p2, int i, String s) {
    String l1 = p1;
    try {
      mayThrow();
      l1 = p2;
      Runnable r =
          (switch (i) {
                case 1 -> "a";
                default -> s;
              })
              ::length;
      l1 = p1;
    } catch (NullPointerException e) {
      // :: error: [assignment]
      @Odd String l3 = l1;
    }
  }

  void methodReferenceMayBeNull(@Odd String p1, String p2, String s) {
    String l1 = p1;
    try {
      mayThrow();
      l1 = p2;
      Runnable r = s::length;
      l1 = p1;
    } catch (NullPointerException e) {
      // :: error: [assignment]
      @Odd String l3 = l1;
    }
  }

  // A call tests its receiver for nullness only after evaluating its arguments (JLS 15.12.4.4), so
  // `l1` is p1 when a null receiver causes a NullPointerException.
  void methodCallMayBeNull(@Odd String p1, String p2, String s) {
    String l1 = p1;
    try {
      mayThrow();
      l1 = p2;
      s.equals(l1 = p1);
    } catch (NullPointerException e) {
      @Odd String l3 = l1;
    }
  }

  class Inner {
    void qualifiedThis(@Odd String p1, String p2) {
      String l1 = p1;
      try {
        mayThrow();
        l1 = p2;
        Runnable r = ReceiverNullPointerException.this::hashCode;
        l1 = p1;
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
