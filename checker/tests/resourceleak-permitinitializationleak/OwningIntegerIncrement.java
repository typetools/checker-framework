// Test case for a postfix increment of an owning field.  The CFG represents the assignment by an
// artificial tree.

import org.checkerframework.checker.mustcall.qual.Owning;

class OwningIntegerIncrement {
  @Owning Integer f = 0;

  void increment() {
    f++;
  }

  void decrement() {
    this.f--;
  }
}
