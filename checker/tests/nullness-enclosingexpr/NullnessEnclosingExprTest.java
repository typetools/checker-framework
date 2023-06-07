import org.checkerframework.checker.initialization.qual.Initialized;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;

class NullnessEnclosingExprTest {
  class InnerWithImplicitEnclosingExpression {
    // There is no possible NPE and therefore no expected error.
    InnerWithImplicitEnclosingExpression() {
      NullnessEnclosingExprTest.this.f.hashCode();
    }
  }

  class InnerWithInitializedEnclosingExpression {
    // The default type of enclosing expression is same as InnerWithImplicitEnclosingExpression,
    // we just make it explicit for testing.
    InnerWithInitializedEnclosingExpression(
        @Initialized NullnessEnclosingExprTest NullnessEnclosingExprTest.this) {}
  }

  class InnerWithUnknownInitializationEnclosingExpression {
    InnerWithUnknownInitializationEnclosingExpression(
        @UnknownInitialization NullnessEnclosingExprTest NullnessEnclosingExprTest.this) {
      // This should also never lead to an NPE, because that dereference should produce an
      // type error.
      // See Issue https://github.com/eisop/checker-framework/issues/412.
      NullnessEnclosingExprTest.this.f.hashCode();
    }
  }

  NullnessEnclosingExprTest() {
    // :: error: (enclosingexpr)
    this.new InnerWithImplicitEnclosingExpression();
    // :: error: (enclosingexpr)
    this.new InnerWithInitializedEnclosingExpression();
    this.new InnerWithUnknownInitializationEnclosingExpression();
    f = "a";
  }

  Object f;
}
