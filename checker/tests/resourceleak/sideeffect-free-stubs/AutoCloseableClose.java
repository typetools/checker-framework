// RLC uses Called Methods facts to remember that a resource has already been closed.
// This test checks that the RLC-specific SideEffectFree stub for AutoCloseable.close()
// preserves those facts across another close call in the same destructor, rather than
// conservatively forgetting them after the first invocation.

import org.checkerframework.checker.calledmethods.qual.EnsuresCalledMethods;
import org.checkerframework.checker.mustcall.qual.Owning;

final class TestAutoCloseable implements AutoCloseable {
  @Override
  public void close() {}
}

class AutoCloseableClose implements AutoCloseable {
  private @Owning AutoCloseable first = new TestAutoCloseable();
  private @Owning AutoCloseable second = new TestAutoCloseable();

  @Override
  @EnsuresCalledMethods(
      value = {"this.first", "this.second"},
      methods = "close")
  public void close() {
    try {
      first.close();
      second.close();
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }
}
