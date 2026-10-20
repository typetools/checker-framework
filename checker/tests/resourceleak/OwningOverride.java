// Test case for https://github.com/typetools/checker-framework/issues/5722

import java.io.*;
import java.net.*;
import org.checkerframework.checker.mustcall.qual.*;

public class OwningOverride {
  abstract static class A {
    public abstract void closeStream(@Owning InputStream s) throws IOException;
  }

  static class B extends A {
    @Override
    // :: error: owning.override.param
    public void closeStream(InputStream s) {}
  }

  static void main(String[] args) throws IOException {
    // no resource leak reported for x
    InputStream x = new FileInputStream("foo.txt");
    A a = new B();
    try {
      a.closeStream(x);
    } catch (Exception e) {
      x.close();
    }
  }
}
