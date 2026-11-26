// Test case for a postfix increment of an owning field.  The CFG represents the assignment by an
// artificial tree.  The increment reassigns the field, so like an assignment such as `f = f + 1`,
// it requires the enclosing method to be @CreatesMustCallFor.

import org.checkerframework.checker.mustcall.qual.Owning;

class OwningIntegerIncrement {
  @Owning Integer f = 0;

  // :: error: (missing.creates.mustcall.for)
  void increment() {
    f++;
  }

  // :: error: (missing.creates.mustcall.for)
  void decrement() {
    this.f--;
  }
}
