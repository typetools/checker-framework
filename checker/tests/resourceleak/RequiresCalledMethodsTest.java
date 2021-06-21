import java.io.*;
import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

public class RequiresCalledMethodsTest {

  @MustCall("a") static class Foo {
    void a() {}

    void c() {}
  }

  @MustCall("releaseFoo") static class FooField {
    private @Owning Foo foo = null;

    @RequiresCalledMethods(
        value = {"this.foo"},
        methods = {"a"})
    @CreatesMustCallFor("this")
    void overwriteFooCorrect() {
      this.foo = new Foo();
    }

    @CreatesMustCallFor("this")
    void overwriteFooWrong() {
      // :: error: required.method.not.called
      this.foo = new Foo();
    }

    @CreatesMustCallFor("this")
    void overwriteFooWithoutReleasing() {
      // :: error: contracts.precondition
      overwriteFooCorrect();
    }

    void releaseThenOverwriteFoo() {
      releaseFoo();
      // :: error: reset.not.owning
      overwriteFooCorrect();
    }

    @EnsuresCalledMethods(
        value = {"this.foo"},
        methods = {"a"})
    void releaseFoo() {
      if (this.foo != null) {
        foo.a();
      }
    }
  }
}
