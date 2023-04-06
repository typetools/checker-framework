// This test case checks for the case where in a superclass a method is pure, but in
// a subclass it is not. In this case, WPI shouldn't infer purity annotations for either
// the superclass or the subclass, because they are unverifiable.

import java.util.Random;

public class OverrideIncompatiblePurity {

  interface MyInterface {
    // WPI should not infer @Pure for this unless all implementations are pure.
    void method();
  }

  class MyImplementation implements MyInterface {

    int field;

    @java.lang.Override
    public void method() {
      // Side effect!
      field = 5;
    }
  }

  class Foo {

    // This implementation is pure, but an overriding implementation in Bar is not.
    String getA(int x) {
      return "A";
    }
  }

  class Bar extends Foo {

    String y;

    // This implementation is neither deterministic nor side-effect free.
    @java.lang.Override
    String getA(int x) {
      if (new Random().nextInt(5) > x) {
        return "B";
      } else {
        y = "C";
        return y;
      }
    }
  }
}
