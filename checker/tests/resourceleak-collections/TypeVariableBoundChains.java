// A collection whose element type is a type variable is a resource collection if the type
// variable's bound has must-call methods, even if the bound is another type variable, or if the
// use of the type variable has must-call methods.

import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.mustcall.qual.*;

class TypeVariableBoundChains {

  // The bound of `T` is the type variable `R`, whose bound is `Resource`.
  static class Pool<R extends Resource, T extends R> {
    // :: error: unfulfilled.field.obligations
    final List<T> items = new ArrayList<>();
  }

  // The bound of `T` has no must-call methods, but the element type `@MustCall("close") T` does.
  @InheritableMustCall("shutdown")
  static class AnnotatedUse<T> {
    // :: error: unfulfilled.field.obligations
    final List<@MustCall("close") T> items = new ArrayList<>();

    void shutdown() {}
  }
}
