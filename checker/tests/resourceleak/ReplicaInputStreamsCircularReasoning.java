// A test case for https://github.com/typetools/checker-framework/issues/4838.
// This variant tries to resolve the obligations of an owning field in its destructor
// by assigning it to another owning field, which shouldn't be permitted.

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import org.checkerframework.checker.calledmethods.qual.EnsuresCalledMethods;
import org.checkerframework.checker.mustcall.qual.*;

class ReplicaInputStreamsCircularReasoning implements Closeable {

  private @Owning InputStream in1;
  private @Owning InputStream in2;

  private ReplicaInputStreamsCircularReasoning other;

  public ReplicaInputStreamsCircularReasoning(@Owning InputStream i1, @Owning InputStream i2) {
    if (this.in1 != null || this.in2 != null) {
      throw new Error("error");
    }
    this.in1 = i1;
    this.in2 = i2;
  }

  @Override
  @EnsuresCalledMethods(
      value = {"this.in1", "this.in2"},
      methods = {"close"})
  @CreatesMustCallFor("#1")
  // :: error: required.method.not.called
  public void close() throws IOException {
    try {
      in1.close();
      in2.close();
    } catch (IOException io) {
      // Definitely not closing the streams or resolving their obligations,
      // but instead trying to trick the analysis.
      other.in1 = this.in1;
      other.in2 = this.in2;
    }
  }
}
