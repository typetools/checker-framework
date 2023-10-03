import java.net.*;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

public class BindFail {

  @InheritableMustCall("a")
  class Foo {
    void a() {}
  }

  @InheritableMustCall("a")
  class Bar {
    @Owning Foo f;

    // :: error: inconsistent.constructor.type
    public @MustCall({}) Bar() {
      // nothing to do
    }

    public Bar(int i) throws Exception {
      throw new Exception();
    }

    @CreatesMustCallFor
    public void overwrite() throws Exception {
      f.a();
      f = new Foo();
      throw new Exception();
    }

    @EnsuresCalledMethods(
        value = {"this.f"},
        methods = {"a"})
    void a() {
      this.f.a();
    }
  }

  public void test() throws Exception {
    // :: error: required.method.not.called
    Socket s = new Socket();
    SocketAddress addr = new InetSocketAddress("127.0.0.1", 6010);
    try {
      s.bind(addr);
    } catch (Exception e) {
      // socket might still be open on this path
      return;
    }
    s.close();
  }

  public void test2() throws Exception {
    // :: error: required.method.not.called
    Bar b = new Bar();
    try {
      b.overwrite();
    } catch (Exception e) {
      return;
    }
    b.a();
  }

  public Bar test3() throws Exception {
    // :: error: required.method.not.called
    Bar b = new Bar();
    try {
      b.overwrite();
      return b;
    } catch (Exception e) {
    }
    return null;
  }

  public Bar test4() throws Exception {
    try {
      // :: error: required.method.not.called
      Bar c = new Bar();
      c.overwrite();
      return c;
    } catch (Exception e) {

    }
    return null;
  }
}
