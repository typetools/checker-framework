// RLC uses Called Methods facts to remember that a resource has already been closed.
// This test checks that the RLC-specific SideEffectFree stub for Closeable.close()
// preserves those facts across another close call in the same destructor, rather than
// conservatively forgetting them after the first invocation.

import java.io.Closeable;
import java.io.IOException;
import org.checkerframework.checker.calledmethods.qual.EnsuresCalledMethods;
import org.checkerframework.checker.mustcall.qual.Owning;

final class TestCloseable implements Closeable {
  @Override
  public void close() {}
}

class CloseableClose implements Closeable {
  private @Owning Closeable first = new TestCloseable();
  private @Owning Closeable second = new TestCloseable();

  @Override
  @EnsuresCalledMethods(
      value = {"this.first", "this.second"},
      methods = "close")
  // The exceptional postcondition is still expected to fail: if first.close() throws,
  // then second.close() is not reached.
  // :: error: contracts.exceptional.postcondition
  public void close() {
    try {
      first.close();
      second.close();
    } catch (IOException e) {
      throw new AssertionError(e);
    }
  }
}
