// test for the crash in https://github.com/typetools/checker-framework/issues/6576

import org.checkerframework.checker.mustcall.qual.*;
import org.checkerframework.checker.calledmethods.qual.*;

import java.net.Socket;

@SuppressWarnings("all") // only check for crashes
public class Issue6576 {

  @MustCall()
  class NoMustCall {
  }

  @MustCall("close")
  class ExtendsMustCallEmpty extends NoMustCall {
    @Owning Socket socket;

    @EnsuresCalledMethods(value = "this.socket", methods = "close")
    void close() {
      try{
        socket.close();
      }
      catch (Exception e){

      }
    }
  }
}
