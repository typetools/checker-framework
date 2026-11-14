// Test case for issue #4105: https://github.com/typetools/checker-framework/issues/4105
// When a local variable gets a new value, or when a new variable with the same name comes into
// scope, dependent type annotations that mention the variable's name are no longer valid.

import java.util.function.IntUnaryOperator;
import org.checkerframework.checker.index.qual.LTLengthOf;
import org.checkerframework.checker.index.qual.LessThan;

public class DependentTypeReassignment {
  void reassign() {
    int b = 10;
    int a = b - 1;
    b = 3;
    // :: error: (assignment)
    @LessThan("b") int c = a;
  }

  void reassign2(int x, int y) {
    int b = x;
    int a = b - 1;
    b = y;
    // :: error: (assignment)
    @LessThan("b") int c = a;
  }

  void noReassignment(int x) {
    int b = x;
    int a = b - 1;
    @LessThan("b") int c = a;
  }

  void redeclare(int x, int y) {
    int a = 5;
    {
      int b = x;
      a = b - 1;
      @LessThan("b") int c = a;
    }
    {
      int b = y;
      // :: error: (assignment)
      @LessThan("b") int c = a;
    }
  }

  void reassignArray(int[] x, int[] y, int i) {
    int[] a = x;
    if (i < a.length) {
      @LTLengthOf("a") int j = i;
      a = y;
      // :: error: (assignment)
      @LTLengthOf("a") int k = i;
    }
  }

  void lambdaOk(int x) {
    int b = x;
    int a = b - 1;
    IntUnaryOperator f =
        y -> {
          @LessThan("b") int c = a;
          return c;
        };
  }

  void anonParam(int x) {
    int a = 5;
    {
      int b = x;
      a = b - 1;
    }
    int aa = a;
    Object o =
        new Object() {
          int m(int b) {
            // :: error: (assignment)
            @LessThan("b") int c = aa;
            return c;
          }
        };
  }

  void anonRedeclared(int x, int y) {
    int aa;
    {
      int b = x;
      aa = b - 1;
    }
    {
      int b = y;
      Object o =
          new Object() {
            int m() {
              // :: error: (assignment)
              @LessThan("b") int c = aa;
              return c;
            }
          };
    }
  }

  void anonOk(int x) {
    int b = x;
    int a = b - 1;
    Object o =
        new Object() {
          int m() {
            @LessThan("b") int c = a;
            return c;
          }
        };
  }

  void declNoInit(int x) {
    int a = 5;
    {
      int b = x;
      a = b - 1;
    }
    {
      int b;
      // :: error: (assignment)
      @LessThan("b") int c = a;
      b = 0;
    }
  }

  void increment(int x) {
    int b = x;
    int a = b - 1;
    b--;
    // :: error: (assignment)
    @LessThan("b") int c = a;
  }

  void compound(int x) {
    int b = x;
    int a = b - 1;
    b -= 2;
    // :: error: (assignment)
    @LessThan("b") int c = a;
  }
}
