// Test case that shows that the fix for
// https://github.com/typetools/checker-framework/issues/6276
// is not fooled by a must-call method that closes the owned field
// of another instance of the class.

import java.io.*;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

@InheritableMustCall("close")
public class SneakyDestructor {
  // :: error: required.method.not.called
  private final @Owning Closeable resource;

  public SneakyDestructor(Closeable r) {
    this.resource = r;
  }

  // ...

  @EnsuresCalledMethods(value = "#1.resource", methods = "close")
  public void close(SneakyDestructor other) throws IOException {
    other.resource.close();
  }
}
