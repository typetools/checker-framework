// Test case for an enhanced for loop over an object whose iterator() method is
// @CreatesMustCallFor.  The CFG represents the invocation of iterator() by an artificial tree.

import java.io.Closeable;
import java.util.Collections;
import java.util.Iterator;
import org.checkerframework.checker.mustcall.qual.CreatesMustCallFor;
import org.checkerframework.checker.mustcall.qual.InheritableMustCall;

@InheritableMustCall("close")
class CreatesMustCallForIterator implements Iterable<String>, Closeable {

  @Override
  @CreatesMustCallFor("this")
  // :: error: (creates.mustcall.for.override.invalid)
  public Iterator<String> iterator() {
    return Collections.emptyIterator();
  }

  @Override
  public void close() {}

  static void closed() {
    CreatesMustCallForIterator rc = new CreatesMustCallForIterator();
    for (String s : rc) {}
    rc.close();
  }

  CreatesMustCallForIterator() {
    for (String s : this) {}
  }

  void loopOverThis() {
    // :: error: (reset.not.owning)
    for (String s : this) {}
  }

  @CreatesMustCallFor("#1")
  static void createsMustCallForParameter(CreatesMustCallForIterator p) {
    for (String s : p) {}
  }
}
