// test for https://github.com/typetools/checker-framework/issues/5777

import java.io.Closeable;
import java.io.IOException;
import java.net.Socket;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

public class MustCallAliasNullConstructor implements Closeable {
  @Owning Socket socket;

  @MustCallAlias MustCallAliasNullConstructor(@MustCallAlias Socket s) throws IOException {
    if (this.socket != null) {
      this.socket.close();
    }
    this.socket = s;
    // :: error: required.method.not.called
    this.socket = null;
  }

  @Override
  @EnsuresCalledMethods(value = "this.socket", methods = "close")
  public void close() throws IOException {
    this.socket.close();
  }
}
