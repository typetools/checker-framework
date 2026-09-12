// @skip-test until the bug is fixed

import org.checkerframework.framework.testchecker.h1h2checker.quals.*;

public class Issue282 {
  // Declared constructor type is not consistent with default from class.
  @SuppressWarnings({"super.invocation", "inconsistent.constructor.type"})
  @H1S1 Issue282() {}

  public class Inner {
    Inner(@H2S2 Issue282 Issue282.this) {}

    // Test anonymous constructor without varargs
    Inner(@H2S2 Issue282 Issue282.this, String s) {}

    Inner(@H2S2 Issue282 Issue282.this, int... i) {}

    Inner(@H2S2 Issue282 Issue282.this, Issue282 o) {}

    Inner(@H2S2 Issue282 Issue282.this, Issue282... o) {}
  }

  @SuppressWarnings({"cast.unsafe.constructor.invocation"})
  public void test1() {
    Inner inner1 = new @H2S2 Issue282().new Inner() {};
    Inner inner2 = new @H2S2 Issue282().new Inner();
    // The enclosing type is @H1S1 @H2Top, while the required type is @H1Top @H2S2
    // :: error: (enclosingexpr)
    Inner inner3 = new Issue282().new Inner() {};
    // :: error: (enclosingexpr)
    Inner inner4 = new Issue282().new Inner();

    // test non-varargs
    Inner inner5 = new @H2S2 Issue282().new Inner("s") {};
    Inner inner6 = new @H2S2 Issue282().new Inner("s");
    // :: error: (enclosingexpr)
    Inner inner7 = new Issue282().new Inner("s") {};
    // :: error: (enclosingexpr)
    Inner inner8 = new Issue282().new Inner("s");

    // test varargs
    Inner inner9 = new @H2S2 Issue282().new Inner(1, 2, 3) {};
    Inner inner10 = new @H2S2 Issue282().new Inner(1, 2, 3);
    // :: error: (enclosingexpr)
    Inner inner11 = new Issue282().new Inner(1, 2, 3) {};
    // :: error: (enclosingexpr)
    Inner inner12 = new Issue282().new Inner(1, 2, 3);

    // test varargs with the same type of receiver
    Inner inner13 = new @H2S2 Issue282().new Inner(this, this, this) {};
    Inner inner14 = new @H2S2 Issue282().new Inner(this, this, this);
    // :: error: (enclosingexpr)
    Inner inner15 = new Issue282().new Inner(this, this, this) {};
    // :: error: (enclosingexpr)
    Inner inner16 = new Issue282().new Inner(this, this, this);
  }

  class Issue282Sub extends Issue282 {}

  public void test2() {
    // found: @H1Top @H2Top Issue282.@H1Top @H2Top Issue282Sub. required: @H1Top @H2S2 Issue282
    // :: error: (enclosingexpr)
    Inner inner = new Issue282Sub().new Inner();
  }

  public static void testStatic() {
    // :: error: (enclosingexpr)
    new Issue282().new Inner() {};
  }

  class InnerGeneric<T> {
    @SuppressWarnings("unchecked")
    InnerGeneric(T... t) {}
  }

  public void test3(@H1S1 String a, @H1S1 String b, @H1S2 String c) {
    new InnerGeneric<@H1S1 String>(a, b);
    // found: @H1S2 @H2Top String. required: @H1S1 @H2Top String
    // :: error: (argument)
    new InnerGeneric<@H1S1 String>(a, c);
  }
}

class Top {
  void test(@H1Top @H2S2 Issue282 outer) {
    outer.new Inner() {};
    outer.new Inner("s") {};
    outer.new Inner(1, 2, 3) {};
    outer.new Inner(outer) {};
    outer.new Inner(outer, outer, outer) {};
  }
}
