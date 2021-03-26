import org.checkerframework.checker.index.qual.IndexFor;
import org.checkerframework.checker.index.qual.IndexOrHigh;
import org.checkerframework.common.value.qual.MinLen;

public class MinLenIndexFor {
  int @MinLen(2) [] arrayLen2 = {0, 1, 2};

  void test(@IndexFor("this.arrayLen2") int i) {
    int j = arrayLen2[i];
    int j2 = arrayLen2[1];
  }

  void callTest(int x) {
    test(0);
    test(1);
    // :: error: (argument.type.incompatible)
    test(2);
    // :: error: (argument.type.incompatible)
    test(3);
    test(arrayLen2.length - 1);
  }

  int @MinLen(4) [] arrayLen4 = {0, 1, 2, 4, 5};

  void test2(@IndexOrHigh("this.arrayLen4") int i) {
    if (i > 0) {
      int j = arrayLen4[i - 1];
    }
    int j2 = arrayLen4[1];
  }

  void callTest2(int x) {
    test2(0);
    test2(1);
    test2(2);
    test2(4);
    // :: error: (argument.type.incompatible)
    test2(5);
    test2(arrayLen4.length);
  }
}
