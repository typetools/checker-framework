// Test case for https://github.com/typetools/checker-framework/issues/5911

import java.io.*;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

class MultipleOwnedResources implements Closeable {

  private final @Owning Closeable r1;
  private final @Owning Closeable r2;

  public MultipleOwnedResources(@Owning Closeable r1, @Owning Closeable r2) {
    this.r1 = r1;
    this.r2 = r2;
  }

  @Override
  @EnsuresCalledMethods(
      value = {"r1", "r2"},
      methods = {"close"})
  public void close() throws IOException {
    try {
      r1.close();
    } finally {
      r2.close();
    }
  }
}
