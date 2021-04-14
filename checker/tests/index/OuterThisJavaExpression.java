// Test case for issue #4558: https://tinyurl.com/cfissue/4558

// @skip-test until the issue is fixed

import org.checkerframework.checker.index.qual.SameLen;

public abstract class OuterThisJavaExpression {

  String s;

  OuterThisJavaExpression(String s) {
    this.s = s;
  }

  final class Inner {

    @SameLen("s") String f1() {
      return s;
    }

    @SameLen("OuterThisJavaExpression.this.s") String f2() {
      return OuterThisJavaExpression.this.s;
    }
  }
}
