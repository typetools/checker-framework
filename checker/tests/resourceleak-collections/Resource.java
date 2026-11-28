// A class that represents an autoclosable resource class, used by other test files.
import org.checkerframework.checker.mustcall.qual.*;

@InheritableMustCall({"flush", "close"})
class Resource implements AutoCloseable {
  @Override
  public void close() {}

  void flush() {}
}
