import java.io.IOException;
import java.net.Socket;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

public class TryWithResourcesMultiResources {

  class OuterResource implements java.io.Closeable {
    private final @Owning Socket socket;

    public @MustCallAlias OuterResource(@MustCallAlias Socket sock) throws IOException {
      this.socket = sock;
    }

    @Override
    @EnsuresCalledMethods(
        value = {"this.socket"},
        methods = {"close"})
    public void close() throws IOException {
      this.socket.close();
    }
  }

  // If "new OuterResource" throws an exception, then the socket won't be released.
  public void multiResourcesWrong(String address, int port) {
    // :: error: required.method.not.called
    try (OuterResource outer = new OuterResource(new Socket(address, port))) {

    } catch (Exception e) {

    }
  }

  public void multiResourcesCorrect(String address, int port) {
    try (Socket s = new Socket(address, port);
        OuterResource outer = new OuterResource(s)) {

    } catch (Exception e) {

    }
  }
}
