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

  class ArrayInner {
    @EnsuresNonNull("#1")
    ArrayInner(Object @Nullable ... args) {
      if (args == null) {
        throw new Error();
      }
    }
  }

  static class Box {
    @Nullable Object f;
  }

  class FieldInner {
    @EnsuresNonNull("#1.f")
    FieldInner(Box b) {
      b.f = new Object();
    }
  }

  void field(AnonymousEnclosingExpressionPostcondition outer, Box b) {
    outer.new FieldInner(b) {};
    b.f.toString();
  }

  void noVarargs(AnonymousEnclosingExpressionPostcondition outer, @Nullable Object c) {
    outer.new Inner(c) {};
    c.toString();
  }

  void someVarargs(AnonymousEnclosingExpressionPostcondition outer, @Nullable Object c) {
    outer.new Inner(c, "a", "b") {};
    c.toString();
  }

  @SuppressWarnings("argument") // The Nullness Checker does not permit covariant array assignment.
  void arrayAsVarargs(AnonymousEnclosingExpressionPostcondition outer, String[] @Nullable [] args) {
    // :: warning: non-varargs call of varargs method with inexact argument type for last
    // parameter;
    outer.new ArrayInner(args) {};
    args.toString();
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
