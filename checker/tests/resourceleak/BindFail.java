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

    // TODO: I think this will warn (in a stub file for Sockets)
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

  public void test2222() throws Exception {
    // :: error: required.method.not.called
    Bar b = new Bar();
    try {
      b.overwrite();
    } catch (Exception e) {
      return;
    }
    b.a();
  }

  public Bar test3333() throws Exception {
    // :: error: required.method.not.called
    Bar bbbbbbb = new Bar();
    try {
      bbbbbbb.overwrite();
      return bbbbbbb;
    } catch (Exception e) {
    }
    return null;
  }

  public Bar test4444() throws Exception {
    try {
      // :: error: required.method.not.called
      Bar ccccc = new Bar();
      ccccc.overwrite();
      return ccccc;
    } catch (Exception e) {

    }
    return null;
  }
}
