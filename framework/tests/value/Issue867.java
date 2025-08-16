// Test case for Issue 867:
// https://github.com/typetools/checker-framework/issues/867

import org.checkerframework.common.value.qual.*;

public class Issue867 {
  void test1() {
    @IntVal({0, 1}) int x1 = 0;
    @IntVal(0) int zero = x1++;
    @IntVal(1) int one = x1;
    // :: error: (unary.increment)
    x1++;

    x1 = 1;
    one = x1--;
    zero = x1;
    // :: error: (unary.decrement)
    x1--;
  }

  void test2() {
    @IntVal({0, 1, 2}) int x2 = 0;
    @IntVal(1) int one = x2++ + x2++;
    @IntVal(2) int two = x2;
    // :: error: (unary.increment)
    x2++;

    x2 = 2;
    @IntVal(3) int three = x2-- + x2--;
    @IntVal(0) int zero = x2;
    // :: error: (unary.decrement)
    x2--;
  }

  void test3() {
    @IntVal({0, 1, 2}) int x3 = 0;
    @IntVal(2) int two = x3++ + ++x3;
    two = x3;
    // :: error: (unary.increment)
    x3++;

    x3 = 2;
    two = x3-- + --x3;
    @IntVal(0) int zero = x3;
    // :: error: (unary.decrement)
    x3--;
  }

  void test4() {
    @IntVal({0, 1}) int x4 = 0;
    m0(x4++);
    // :: error: (argument)
    m0(x4);
    // :: error: (unary.increment)
    m1(x4++);

    x4 = 1;
    m1(x4--);
    // :: error: (argument)
    m1(x4);
    // :: error: (unary.decrement)
    m0(x4--);
  }

  void m0(@IntVal(0) int x) {}

  void m1(@IntVal(1) int x) {}
}
