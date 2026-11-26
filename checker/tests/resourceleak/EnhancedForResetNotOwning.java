// Test case for the location of a reset.not.owning error for an enhanced for loop over an object
// whose iterator() method is @CreatesMustCallFor.  The CFG represents the invocation of
// iterator() by an artificial tree, which has no position in the source code.

import java.io.Closeable;
import java.util.Collections;
import java.util.Iterator;
import org.checkerframework.checker.mustcall.qual.CreatesMustCallFor;
import org.checkerframework.checker.mustcall.qual.InheritableMustCall;

@InheritableMustCall("close")
class EnhancedForResetNotOwning implements Iterable<String>, Closeable {

  @Override
  @CreatesMustCallFor("this")
  // :: error: (creates.mustcall.for.override.invalid)
  public Iterator<String> iterator() {
    return Collections.emptyIterator();
  }

  @Override
  public void close() {}

  static void notOwning(EnhancedForResetNotOwning p) {
    // :: error: (reset.not.owning)
    for (String s : p) {}
  }
}
