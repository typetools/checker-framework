// A constructor postcondition at `outer.new Inner(...) {}`, where the constructor has varargs.

import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class AnonymousEnclosingExpressionPostcondition {

  class Inner {
    @EnsuresNonNull("#1")
    Inner(@Nullable Object c, Object... rest) {
      if (c == null) {
        throw new Error();
      }
    }
  }

  void noVarargs(AnonymousEnclosingExpressionPostcondition outer, @Nullable Object c) {
    outer.new Inner(c) {};
    c.toString();
  }

  void someVarargs(AnonymousEnclosingExpressionPostcondition outer, @Nullable Object c) {
    outer.new Inner(c, "a", "b") {};
    c.toString();
  }

  void implicitEnclosing(@Nullable Object c) {
    new Inner(c) {};
    c.toString();
  }

  void notAnonymous(AnonymousEnclosingExpressionPostcondition outer, @Nullable Object c) {
    outer.new Inner(c);
    c.toString();
  }
}
