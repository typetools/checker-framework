// A collection whose element type is a type variable is a resource collection if the type
// variable's bound has must-call methods, even if the bound is another type variable.

import java.util.ArrayList;
import java.util.List;

class TypeVariableBoundChains {

  // The bound of `T` is the type variable `R`, whose bound is `Resource`.
  static class Pool<R extends Resource, T extends R> {
    // :: error: unfulfilled.field.obligations
    final List<T> items = new ArrayList<>();
  }
}
