// The CFG must convert each argument of `outer.new Inner(...) {}` against the matching formal
// parameter, not against the anonymous constructor's enclosing-instance parameter.

import org.checkerframework.checker.nullness.qual.Nullable;

public class AnonymousClassEnclosingExpressionArgs {
  class Inner {
    Inner(int x) {}
  }

  void m(AnonymousClassEnclosingExpressionArgs outer, @Nullable Integer i) {
    // :: error: (unboxing.of.nullable)
    Object o = outer.new Inner(i) {};
    // The unboxing refines `i` to non-null.
    i.toString();
  }
}
