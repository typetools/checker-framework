import java.io.IOException;
import java.net.*;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

class OwnershipTransferOnAnonymousConstructor {
  class Foo {
    Foo(@Owning Socket s, Object... rest) {
      try {
        s.close();
      } catch (IOException e) {

      }
    }
  }

  private class Bar {
    void baz(OwnershipTransferOnAnonymousConstructor outer, Socket s) {
      Foo f = outer.new Foo(s) {};
    }

    // :: warning: [required.method.not.called]
    void testOwningOnBaz(OwnershipTransferOnAnonymousConstructor outer, @Owning Socket s) {
      Socket s2 = s;
      baz(outer, s2);
    }

    void implicitEnclosing(Socket s) {
      Foo f = new Foo(s) {};
    }

    // :: warning: [required.method.not.called]
    void testOwningOnImplicitEnclosing(@Owning Socket s) {
      Socket s2 = s;
      implicitEnclosing(s2);
    }
  }
}
