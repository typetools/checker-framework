// Ownership transfer to the constructor of an anonymous class with an explicit enclosing
// expression, as in `outer.new Inner(...) {}`.

import java.io.IOException;
import java.net.Socket;
import org.checkerframework.checker.calledmethods.qual.EnsuresCalledMethods;
import org.checkerframework.checker.mustcall.qual.InheritableMustCall;
import org.checkerframework.checker.mustcall.qual.Owning;

public class AnonymousEnclosingExpressionOwning {

  @InheritableMustCall("close")
  class Inner implements AutoCloseable {
    final @Owning Socket socket;

    Inner(@Owning Socket socket) {
      this.socket = socket;
    }

    @Override
    @EnsuresCalledMethods(value = "this.socket", methods = "close")
    public void close() throws IOException {
      socket.close();
    }
  }

  void explicitEnclosing(AnonymousEnclosingExpressionOwning outer, @Owning Socket s)
      throws IOException {
    Inner i = outer.new Inner(s) {};
    i.close();
  }

  void implicitEnclosing(@Owning Socket s) throws IOException {
    Inner i = new Inner(s) {};
    i.close();
  }

  void notAnonymous(AnonymousEnclosingExpressionOwning outer, @Owning Socket s) throws IOException {
    Inner i = outer.new Inner(s);
    i.close();
  }

  void leak(AnonymousEnclosingExpressionOwning outer, @Owning Socket s) {
    // :: error: (required.method.not.called)
    Inner i = outer.new Inner(s) {};
  }
}
