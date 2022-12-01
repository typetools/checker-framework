import org.checkerframework.checker.mustcall.qual.*;
import org.checkerframework.checker.calledmethods.qual.*;

@InheritableMustCall({"finish1", "finish2"})
class TwoOwningMCATest {

  @Owning private final Foo f1 = new Foo();

  @Owning private final Foo f2;

  @MustCallAlias
  TwoOwningMCATest(@MustCallAlias Foo g) {
    this.f2 = g;
  }

  @EnsuresCalledMethods(value="this.f1", methods="a")
  void finish1() {
    this.f1.a();
  }

  @EnsuresCalledMethods(value="this.f2", methods="a")
  void finish2() {
    this.f2.a();
  }

  @InheritableMustCall("a")
  static class Foo {
    void a() {}
  }

  public static void test(Foo f) {
    // :: error: required.method.not.called
    TwoOwningMCATest t = new TwoOwningMCATest(f);
    f.a();
  }
}
