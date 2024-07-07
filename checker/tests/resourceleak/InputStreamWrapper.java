import java.io.*;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

@InheritableMustCall("dispose")
class InputStreamWrapper {
  private final @Owning InputStream stream;

  @MustCallAlias InputStreamWrapper(@MustCallAlias InputStream stream) {
    this.stream = stream;
  }

  @EnsuresCalledMethods(value = "this.stream", methods = "close")
  public void dispose() throws IOException {
    this.stream.close();
  }

  /** Shows that either the stream or the wrapper can be closed. */
  static void test(@Owning InputStream stream, boolean b) throws IOException {
    InputStreamWrapper wrapper = new InputStreamWrapper(stream);
    if (b) {
      stream.close();
    } else {
      wrapper.dispose();
    }
  }
}
