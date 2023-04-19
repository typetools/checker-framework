// Test case for https://github.com/typetools/checker-framework/issues/5762

import java.io.IOException;
import java.net.Socket;
import org.checkerframework.checker.calledmethods.qual.EnsuresCalledMethods;
import org.checkerframework.checker.mustcall.qual.InheritableMustCall;
import org.checkerframework.checker.mustcall.qual.Owning;

@InheritableMustCall("close")
public class InitializationAfterSuperTest implements AutoCloseable {

  @Owning Socket mySocket;

  public InitializationAfterSuperTest(@Owning Socket mySocket) throws IOException {
    super();
    if (this.mySocket == null) {
      this.mySocket = mySocket;
    } else {
      mySocket.close();
    }
  }

  @EnsuresCalledMethods(value = "mySocket", methods = "close")
  @Override
  public void close() throws IOException {
    mySocket.close();
  }
}
