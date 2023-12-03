import java.io.*;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;
import org.checkerframework.checker.nullness.qual.*;

abstract class TestCase {

  abstract @Nullable Closeable alloc();

  abstract boolean arbitraryChoice();

  void method() throws IOException {

    if (arbitraryChoice()) {
      return;
    }

    Closeable r1 = alloc();
    if (r1 == null) {
      return;
    }
    r1.close();
  }
}
