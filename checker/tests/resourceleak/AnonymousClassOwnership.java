// Ownership transfer to the constructor of an anonymous class, including one with an explicit
// enclosing expression, as in `outer.new Inner(...) {}`.

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import org.checkerframework.checker.calledmethods.qual.EnsuresCalledMethods;
import org.checkerframework.checker.mustcall.qual.CreatesMustCallFor;
import org.checkerframework.checker.mustcall.qual.InheritableMustCall;
import org.checkerframework.checker.mustcall.qual.MustCallAlias;
import org.checkerframework.checker.mustcall.qual.Owning;
import org.checkerframework.checker.nullness.qual.Nullable;

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

  class TwoParams {
    // :: error: (required.method.not.called)
    TwoParams(Closeable notOwned, @Owning Closeable owned) {}
  }

  // Only the argument for the @Owning parameter is transferred.
  void twoParams(
      AnonymousClassOwnership outer,
      // :: error: (required.method.not.called)
      @Owning Closeable notOwned,
      @Owning Closeable owned) {
    outer.new TwoParams(notOwned, owned) {};
  }

  class NotOwning {
    NotOwning(Closeable c) {}
  }

  void notOwning(
      AnonymousClassOwnership outer,
      // :: error: (required.method.not.called)
      @Owning Closeable c) {
    outer.new NotOwning(c) {};
  }

  class OnlyVarargs {
    OnlyVarargs(Closeable... cs) {}
  }

  void onlyVarargs(AnonymousClassOwnership outer, Closeable a, Closeable b) {
    outer.new OnlyVarargs() {};
    outer.new OnlyVarargs(a, b) {};
  }

  static class Nested {
    // :: error: (required.method.not.called)
    Nested(@Owning Closeable c) {}
  }

  void nested(@Owning Closeable c) {
    new Nested(c) {};
  }

  @InheritableMustCall("disconnect")
  static class Holder {

    private @Owning @Nullable Node head = null;

    @CreatesMustCallFor("this")
    void addNamed() {
      head = new Node(head);
    }

    @CreatesMustCallFor("this")
    void addAnonymous() {
      head = new Node(head) {};
    }

    @EnsuresCalledMethods(value = "this.head", methods = "disconnect")
    void disconnect() {
      if (head != null) {
        head.disconnect();
      }
    }
  }

  @InheritableMustCall("disconnect")
  static class Node {
    private final @Owning @Nullable Node next;

    Node(@Owning @Nullable Node next) {
      this.next = next;
    }

    @EnsuresCalledMethods(value = "this.next", methods = "disconnect")
    void disconnect() {
      if (next != null) {
        next.disconnect();
      }
    }
  }
}
