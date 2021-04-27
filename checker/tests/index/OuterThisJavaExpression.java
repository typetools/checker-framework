// Test case for issue #4558: https://tinyurl.com/cfissue/4558

// @skip-test until the issue is fixed

import org.checkerframework.checker.index.qual.SameLen;

public abstract class OuterThisJavaExpression {

  String s;

  OuterThisJavaExpression(String s) {
    this.s = s;
  }

  final class Inner {

    String s = "different from " + OuterThisJavaExpression.this.s;

    @SameLen("s") String f1() {
      return s;
    }

    @SameLen("s") String f2() {
      return this.s;
    }

    @SameLen("s") String f3() {
      // :: error: (return.type.incompatible)
      return OuterThisJavaExpression.this.s;
    }

    @SameLen("this.s") String f4() {
      return s;
    }

    @SameLen("this.s") String f5() {
      return this.s;
    }

    @SameLen("this.s") String f6() {
      // :: error: (return.type.incompatible)
      return OuterThisJavaExpression.this.s;
    }

    @SameLen("OuterThisJavaExpression.this.s") String f7() {
      // :: error: (return.type.incompatible)
      return s;
    }

    @SameLen("OuterThisJavaExpression.this.s") String f8() {
      // :: error: (return.type.incompatible)
      return this.s;
    }

    @SameLen("OuterThisJavaExpression.this.s") String f9() {
      return OuterThisJavaExpression.this.s;
    }
  }
}
