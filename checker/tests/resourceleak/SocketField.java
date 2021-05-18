// test case for https://github.com/kelloggm/object-construction-checker/issues/381

import java.io.IOException;
import java.net.Socket;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;
import org.checkerframework.dataflow.qual.Pure;

@MustCall("closeSocket") class SocketField {
  protected @Owning Socket socket = null;

  @CreatesObligation("this")
  protected void setupConnection(javax.net.SocketFactory socketFactory) throws IOException {
    // This is the original test case. Before this issue was fixed, an error was issued on the
    // second line.
    this.socket.close();
    this.socket = socketFactory.createSocket();
  }

  @CreatesObligation("this")
  protected void setupConnectionWithLocal(javax.net.SocketFactory socketFactory)
      throws IOException {
    // This is the original test case, modified to include an assignment to a local that
    // demonstrates that
    // the correct value was in the store at some point.
    this.socket.close();
    @CalledMethods("close") Socket s = this.socket;
    this.socket = socketFactory.createSocket();
  }

  @CreatesObligation("this")
  protected void setupConnectionWithConstructor(javax.net.SocketFactory socketFactory)
      throws IOException {
    // This is the original test case, modified to replace the call to createSocket() with a new
    // Socket() call.
    // This version succeeded, even before the bug was fixed.
    this.socket.close();
    this.socket = new Socket();
  }

  @CreatesObligation("this")
  protected void setupConnection2(javax.net.SocketFactory socketFactory) throws IOException {
    this.socket.close();
    // This version succeeds, because getSocket is @Pure, so no side-effects can occur.
    this.socket = getSocket(socketFactory);
  }

  @CreatesObligation("this")
  protected void setupConnection3(javax.net.SocketFactory socketFactory) throws IOException {
    // This version demonstrates a work-around.
    Socket s = socketFactory.createSocket();
    this.socket.close();
    this.socket = s;
  }

  @Pure
  private Socket getSocket(javax.net.SocketFactory socketFactory) throws IOException {
    return socketFactory.createSocket();
  }

  @EnsuresCalledMethods(value = "this.socket", methods = "close")
  private void closeSocket() throws IOException {
    this.socket.close();
  }
}
