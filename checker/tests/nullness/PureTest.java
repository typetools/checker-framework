import org.checkerframework.checker.nullness.qual.*;
import org.checkerframework.dataflow.qual.*;

public class PureTest {
  @org.checkerframework.dataflow.qual.Pure
  @Nullable Object puremethod(@Nullable Object a) {
    return a;
  }

  public void test() {
    // :: error: (assignment)
    @NonNull Object l0 = puremethod(null);

    if (puremethod(null) == null) {
      // :: error: (assignment)
      @NonNull Object l1 = puremethod(null);
    }

    if (puremethod("m") != null) {
      @NonNull Object l1 = puremethod("m");
    }

    if (puremethod("m") != null) {
      // :: error: (assignment)
      @NonNull Object l1 = puremethod(null);
    }

    if (puremethod("m") != null) {
      // :: error: (assignment)
      @NonNull Object l1 = puremethod("n");
    }

    Object x = new Object();

    if (puremethod(x) == null) {
      return;
    }

    @NonNull Object l2 = puremethod(x);

    x = new Object();

    // :: error: (assignment)
    @NonNull Object l3 = puremethod(x);

    // :: error: (assignment)
    @NonNull Object l4 = puremethod("n");
  }

  public @org.checkerframework.dataflow.qual.Pure @Nullable Object getSuperclass() {
    return null;
  }

  static void shortCircuitAnd(PureTest pt) {
    if ((pt.getSuperclass() != null) && pt.getSuperclass().equals(Enum.class)) {
      // empty body
    }
  }

  static void shortCircuitOr(PureTest pt) {
    if ((pt.getSuperclass() == null) || pt.getSuperclass().equals(Enum.class)) {
      // empty body
    }
  }

  static void testInstanceofNegative(PureTest pt) {
    if (pt.getSuperclass() instanceof Object) {
      return;
    }
    // :: error: (dereference.of.nullable)
    pt.getSuperclass().toString();
  }

  static void testInstanceofPositive(PureTest pt) {
    if (!(pt.getSuperclass() instanceof Object)) {
      return;
    }
    pt.getSuperclass().toString();
  }

  static void testInstanceofPositive2(PureTest pt) {
    if (!(pt.getSuperclass() instanceof Object)) {
    } else {
      pt.getSuperclass().toString();
    }
  }

  static void testInstanceofNegative2(PureTest pt) {
    if (pt.getSuperclass() instanceof Object) {
    } else {
      return;
    }
    pt.getSuperclass().toString();
  }

  static void testInstanceofString(PureTest pt) {
    if (!(pt.getSuperclass() instanceof String)) {
      return;
    }
    pt.getSuperclass().toString();
  }

  static void testContinue(PureTest pt) {
    for (; ; ) {
      if (pt.getSuperclass() == null) {
        System.out.println("m");
        continue;
      }
      pt.getSuperclass().toString();
    }
  }

  void setSuperclass(@Nullable Object no) {
    // set the field returned by getSuperclass.
  }

  static void testInstanceofPositive3(PureTest pt) {
    if (!(pt.getSuperclass() instanceof Object)) {
      return;
    } else {
      pt.setSuperclass(null);
    }
    // :: error: (dereference.of.nullable)
    pt.getSuperclass().toString();
  }

  @Override
  @SideEffectFree
  public String toString() {
    return "foo";
  }
}
