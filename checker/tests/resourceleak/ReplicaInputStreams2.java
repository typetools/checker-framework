// A test case for https://github.com/typetools/checker-framework/issues/4838.
// This variant uses a try-finally in the destructor, so it is correct.

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import org.checkerframework.checker.calledmethods.qual.EnsuresCalledMethods;
import org.checkerframework.checker.mustcall.qual.Owning;

class ReplicaInputStreams2 implements Closeable {

  private final @Owning InputStream in1;
  private final @Owning InputStream in2;

  public ReplicaInputStreams2(@Owning InputStream i1, @Owning InputStream i2) {
    this.in1 = i1;
    this.in2 = i2;
  }

  @Override
  @EnsuresCalledMethods(
      value = {"this.in1", "this.in2"},
      methods = {"close"})
  public void close() throws IOException {
    try {
      in1.close();
    } finally {
      in2.close();
    }
  }
}
