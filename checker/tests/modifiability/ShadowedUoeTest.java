// Tests that the `method.implementation.not.uoe` check compares the type of the thrown exception
// rather than the name that appears in the source code.

import java.util.AbstractList;
import org.checkerframework.checker.modifiability.qual.Growable;
import org.checkerframework.checker.modifiability.qual.Ungrowable;

public class ShadowedUoeTest {

  /**
   * A different exception that happens to be named UnsupportedOperationException. It shadows
   * java.lang.UnsupportedOperationException throughout this class.
   */
  static class UnsupportedOperationException extends RuntimeException {}

  // All constructors are @Ungrowable, so a method with a @Growable receiver must throw
  // java.lang.UnsupportedOperationException.
  static class UngrowableList extends AbstractList<String> {
    @Ungrowable UngrowableList() {}

    @Override
    public String get(int index) {
      return "value";
    }

    @Override
    public int size() {
      return 0;
    }

    // :: error: [method.implementation.not.uoe]
    public void growThrowsShadowed(@Growable UngrowableList this) {
      throw new UnsupportedOperationException();
    }

    public void growThrowsQualified(@Growable UngrowableList this) {
      throw new java.lang.UnsupportedOperationException();
    }
  }
}
