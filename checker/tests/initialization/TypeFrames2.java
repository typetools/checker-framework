import org.checkerframework.checker.initialization.qual.UnderInitialization;
import org.checkerframework.checker.nullness.qual.NonNull;

public class TypeFrames2 {

  class A {
    @NonNull String a;

    public A() {
      // :: error: (method.invocation.invalid)
      this.foo();
      a = "";
      this.foo();
    }

    public void foo(@UnderInitialization(A.class) A this) {}
  }

  class B extends A {
    @NonNull String b;

    public B() {
      super();
      this.foo();
      // :: error: (method.invocation.invalid)
      this.bar();
      b = "";
      this.bar();
    }

    public void bar(@UnderInitialization(B.class) B this) {}
  }
}
