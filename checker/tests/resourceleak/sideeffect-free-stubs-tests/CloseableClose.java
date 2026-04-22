// RLC uses Called Methods facts to remember that a resource has already been closed.
// This test checks that the RLC-specific SideEffectFree stub for Closeable.close()
// preserves those facts across another close call in the same destructor, rather than
// conservatively forgetting them after the first invocation.

import java.io.Closeable;
import org.checkerframework.checker.calledmethods.qual.EnsuresCalledMethods;
import org.checkerframework.checker.mustcall.qual.Owning;

class CloseableClose implements Closeable {
  private @Owning Closeable first = new CloseableResource();
  private @Owning Closeable second = new CloseableResource();

  @Override
  @EnsuresCalledMethods(
      value = {"this.first", "this.second"},
      methods = "close")
  // This is a false positive warning, because no side effect should unrefine the
  // "@Closed" type of `first`.
  // :: error: [contracts.postcondition]
  public void close() {
    try {
      try {
        first.close();
      } catch (Exception ignored) {
      }
      second.close();
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }
}
