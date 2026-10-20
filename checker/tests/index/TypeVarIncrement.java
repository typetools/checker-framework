// Test case for increments and decrements of a type variable bounded by a primitive wrapper.

import org.checkerframework.checker.index.qual.NonNegative;

public class TypeVarIncrement {
  <T extends Integer> T unannotated(T t) {
    ++t;
    --t;
    t++;
    t--;
    return t;
  }

  <T extends @NonNegative Integer> void decrement(T t) {
    // :: error: [unary.decrement]
    --t;
  }
}
