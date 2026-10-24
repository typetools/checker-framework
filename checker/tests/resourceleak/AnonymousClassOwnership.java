// Ownership transfer to the constructor of an anonymous class, including one with an explicit
// enclosing expression, as in `outer.new Inner(...) {}`.

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import org.checkerframework.checker.calledmethods.qual.EnsuresCalledMethods;
import org.checkerframework.checker.mustcall.qual.InheritableMustCall;
import org.checkerframework.checker.mustcall.qual.MustCallAlias;
import org.checkerframework.checker.mustcall.qual.Owning;

public class AnonymousClassOwnership {

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

  void explicitEnclosing(AnonymousClassOwnership outer, @Owning Socket s) throws IOException {
    Inner i = outer.new Inner(s) {};
    i.close();
  }

  void implicitEnclosing(@Owning Socket s) throws IOException {
    Inner i = new Inner(s) {};
    i.close();
  }

  void notAnonymous(AnonymousClassOwnership outer, @Owning Socket s) throws IOException {
    Inner i = outer.new Inner(s);
    i.close();
  }

  void leak(AnonymousClassOwnership outer, @Owning Socket s) {
    // :: error: (required.method.not.called)
    Inner i = outer.new Inner(s) {};
  }

  @InheritableMustCall("close")
  class VarargsInner implements AutoCloseable {
    final @Owning Socket socket;

    VarargsInner(@Owning Socket socket, Object... rest) {
      this.socket = socket;
    }

    @Override
    @EnsuresCalledMethods(value = "this.socket", methods = "close")
    public void close() throws IOException {
      socket.close();
    }
  }

  void varargs(AnonymousClassOwnership outer, @Owning Socket s) throws IOException {
    VarargsInner i = outer.new VarargsInner(s) {};
    i.close();
  }

  void varargsLeak(AnonymousClassOwnership outer, @Owning Socket s) {
    // :: error: (required.method.not.called)
    VarargsInner i = outer.new VarargsInner(s) {};
  }

  class VarargsWrapper implements Closeable {
    private final @Owning Socket socket;

    @MustCallAlias VarargsWrapper(@MustCallAlias Socket socket, Object... rest) {
      this.socket = socket;
    }

    @Override
    @EnsuresCalledMethods(value = "this.socket", methods = "close")
    public void close() throws IOException {
      socket.close();
    }
  }

  void mustCallAliasVarargs(AnonymousClassOwnership outer, @Owning Socket s) throws IOException {
    VarargsWrapper w = outer.new VarargsWrapper(s) {};
    w.close();
  }

  // :: error: (required.method.not.called)
  void mustCallAliasVarargsLeak(AnonymousClassOwnership outer, @Owning Socket s) {
    VarargsWrapper w = outer.new VarargsWrapper(s) {};
  }

  // BufferedInputStream's constructor has a @MustCallAlias parameter.
  void mustCallAlias(@Owning InputStream in) throws IOException {
    InputStream w = new BufferedInputStream(in) {};
    w.close();
  }

  // :: error: (required.method.not.called)
  void mustCallAliasLeak(@Owning InputStream in) {
    InputStream w = new BufferedInputStream(in) {};
  }
}
