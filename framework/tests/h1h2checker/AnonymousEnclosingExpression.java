// Arguments of an anonymous class with an explicit enclosing expression, as in
// `outer.new Inner(...) {}`, are checked against the right formal parameters.

import org.checkerframework.framework.testchecker.h1h2checker.quals.*;

public class AnonymousEnclosingExpression {
  public class Inner {
    Inner(@H2S2 AnonymousEnclosingExpression AnonymousEnclosingExpression.this) {}

    Inner(@H2S2 AnonymousEnclosingExpression AnonymousEnclosingExpression.this, String s) {}

    Inner(@H2S2 AnonymousEnclosingExpression AnonymousEnclosingExpression.this, int... i) {}

    Inner(
        @H2S2 AnonymousEnclosingExpression AnonymousEnclosingExpression.this,
        AnonymousEnclosingExpression o) {}

    Inner(
        @H2S2 AnonymousEnclosingExpression AnonymousEnclosingExpression.this,
        AnonymousEnclosingExpression... o) {}

    Inner(
        @H2S2 AnonymousEnclosingExpression AnonymousEnclosingExpression.this,
        @H1S1 String s,
        @H1S2 String t) {}
  }

  void test(@H1Top @H2S2 AnonymousEnclosingExpression outer) {
    outer.new Inner() {};
    outer.new Inner("s") {};
    outer.new Inner(1, 2, 3) {};
    outer.new Inner(outer) {};
    outer.new Inner(outer, outer, outer) {};
  }

  static void staticContext(@H2S2 AnonymousEnclosingExpression outer, @H1S1 String s1) {
    outer.new Inner() {};
    outer.new Inner("s") {};
    // :: error: (argument)
    outer.new Inner(s1, s1) {};
  }

  void arguments(@H2S2 AnonymousEnclosingExpression outer, @H1S1 String s1, @H1S2 String s2) {
    outer.new Inner(s1, s2) {};
    // :: error: (argument)
    outer.new Inner(s2, s2) {};
    // :: error: (argument)
    outer.new Inner(s1, s1) {};
  }
}
