// Test case for the location of a reset.not.owning error for a try-with-resources statement whose
// resource's close() method is @CreatesMustCallFor.  The CFG represents the invocation of close()
// by an artificial tree, which has no position in the source code.

import java.io.Closeable;
import org.checkerframework.checker.mustcall.qual.CreatesMustCallFor;
import org.checkerframework.checker.mustcall.qual.InheritableMustCall;

@InheritableMustCall("close")
class TryWithResourcesResetNotOwning implements Closeable {

  @Override
  @CreatesMustCallFor("this")
  // :: error: (creates.mustcall.for.override.invalid)
  public void close() {}

  static void notOwning(TryWithResourcesResetNotOwning p, TryWithResourcesResetNotOwning q) {
    try (
    // :: error: (reset.not.owning)
    p;
        // :: error: (reset.not.owning)
        q) {}
  }
}
