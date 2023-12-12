import java.io.IOException;
import java.net.*;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

class OwnershipTransferOnConstructor {
  static class Foo {
    Foo(@Owning Socket s) {
      try {
        s.close();
      } catch (IOException e) {

      }
    }
  }

  private class Bar {
    void baz(Socket s) {
      Foo f = new Foo(s);
    }

    // :: warning: (required.method.not.called)
    void testOwningOnBaz(@Owning Socket s) {
      Socket s2 = s;
      baz(s2);
    }
  }
}
