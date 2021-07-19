package index;

import org.checkerframework.checker.index.qual.IndexFor;

@SuppressWarnings("upperbound")
public class IndexForTestLBC {
  int[] array = {0};

  void test1(@IndexFor("array") int i) {
    int x = this.array[i];
  }

  void callTest1(int x) {
    test1(0);
    test1(1);
    test1(2);
    test1(array.length);
    // :: error: (argument.type.incompatible)
    test1(array.length - 1);
    if (array.length > x) {
      // :: error: (argument.type.incompatible)
      test1(x);
    }

    if (array.length == x) {
      test1(x);
    }
  }
}
