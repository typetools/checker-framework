// @below-java17-jdk-skip-test
import org.checkerframework.common.value.qual.IntVal;

public class MultiCaseConst {

  void method(int selector) {
    switch (selector) {
      case 1, 2, 3:
        // :: error: (assignment)
        @IntVal(0) int o = selector;
        @IntVal({1, 2, 3}) int tmp = selector;
      case 4, 5:
        // :: error: (assignment)
        @IntVal({4, 5}) int tmp2 = selector;
        @IntVal({1, 2, 3, 4, 5}) int tmp3 = selector;
    }
  }

  void method2(int selector) {
    switch (selector) {
      case 1:
        // :: error: (assignment)
        @IntVal(0) int o = selector;
        @IntVal({1, 2, 3}) int tmp = selector;
        break;
      case 4, 5:
        @IntVal({4, 5}) int tmp2 = selector;
        @IntVal({1, 2, 3, 4, 5}) int tmp3 = selector;
    }
  }
}
