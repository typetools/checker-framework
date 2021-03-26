import org.checkerframework.checker.interning.qual.Interned;

public class CompileTimeConstants {
  class A {
    static final String a1 = "hello";
    @Interned String a2 = "a2";

    void method() {
      if (a1 == "hello") {}
    }
  }

  class B {
    static final String b1 = "hello";

    void method() {
      if (b1 == A.a1) {}
    }
  }
}
