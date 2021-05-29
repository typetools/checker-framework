import org.checkerframework.checker.nullness.qual.*;
import org.checkerframework.checker.nullness.util.Opt;

/** Test class org.checkerframework.checker.nullness.util.Opt. */
public class TestOpt {
  void foo1(@Nullable Object p) {
    if (Opt.isPresent(p)) {
      p.toString(); // Flow refinement
    }
  }

  void foo1b(@Nullable Object p) {
    if (!Opt.isPresent(p)) {
      // :: error: (dereference.of.nullable)
      p.toString();
    }
  }

  void foo2(@Nullable Object p) {
    Opt.ifPresent(p, x -> System.out.println("Got: " + x));
  }

  void foo2b(@Nullable Object p) {
    Opt.ifPresent(p, x -> System.out.println("Got: " + x.toString()));
  }

  void foo3(@Nullable Object p) {
    Object o = Opt.filter(p, x -> x.hashCode() > 10);
  }

  void foo4(@Nullable Object p) {
    String s = Opt.map(p, x -> x.toString());
  }

  void foo4b(@Nullable Object p) {
    // :: error: (argument)
    String s = Opt.map(p, null);
  }

  void foo5(@Nullable Object p) {
    @NonNull Object o = Opt.orElse(p, new Object());
  }

  void foo5b(@Nullable Object p) {
    // :: error: (argument)
    @NonNull Object o = Opt.orElse(p, null);
  }

  void foo6(@Nullable Object p) {
    @NonNull Object o = Opt.orElseGet(p, () -> new Object());
  }

  void foo6b(@Nullable Object p) {
    // :: error: (return)
    @NonNull Object o = Opt.orElseGet(p, () -> null);
  }

  void foo7(Object p) {
    try {
      @NonNull Object o = Opt.orElseThrow(p, () -> new Throwable());
    } catch (Throwable t) {
      // p was null
    }
  }

  void foo7b(@Nullable Object p) {
    try {
      // :: error: (assignment) :: error: (type.argument)
      // :: error: (return)
      @NonNull Object o = Opt.orElseThrow(p, () -> null);
    } catch (Throwable t) {
      // p was null
    }
  }
}
