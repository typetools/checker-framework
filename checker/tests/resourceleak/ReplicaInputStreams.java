import java.io.IOException;
import java.io.InputStream;
import java.io.Closeable;
import org.checkerframework.checker.mustcall.qual.Owning;
import org.checkerframework.checker.calledmethods.qual.EnsuresCalledMethods;

class ReplicaInputStreams implements Closeable {

  private final @Owning InputStream in1;
  private final @Owning InputStream in2;

  public ReplicaInputStreams(
      @Owning InputStream i1, @Owning InputStream i2) {
    this.in1 = i1;
    this.in2 = i2;
  }

  @Override
  @EnsuresCalledMethods(
      value = {"this.in1", "this.in2"},
      methods = {"close"})
  public void close() throws IOException {
    in1.close();
    in2.close();
  }
}
