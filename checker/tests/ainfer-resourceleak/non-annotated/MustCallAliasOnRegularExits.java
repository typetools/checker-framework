import java.io.IOException;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

class MustCallAliasOnRegularExits {

  @InheritableMustCall("a")
  static class Foo {
    void a() {}

    int b() throws IOException {
      return 0;
    }
  }

  private class MCAConstructor extends Foo {

    protected final @Owning Foo f; // expect owning annotation for this field
    protected long s = 0L;

    // The Must Call Checker for assigning @MustCallAlias parameters to @Owning fields reports a false positive.
    @SuppressWarnings("assignment")
    protected MCAConstructor(Foo foo) throws IOException {
      if (foo == null) {
        this.s = foo.b();
      }
      this.f = foo;
    }

    @EnsuresCalledMethods(
        value = {"this.f"},
        methods = {"a"})
    public void a() {
      f.a();
    }
  }

  void testMCAOnMCAConstructor() {
    Foo f = new Foo();
    try {
      // :: warning: (required.method.not.called)
      MCAConstructor mcaf = new MCAConstructor(f);
    } catch (IOException e) {
    } finally {
      f.a();
    }
  }

}
