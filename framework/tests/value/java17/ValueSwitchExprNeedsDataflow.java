// @below-java17-jdk-skip-test
import org.checkerframework.common.value.qual.IntVal;

public class ValueSwitchExprNeedsDataflow {

  void method(int selector) {
    @IntVal({2, 3}) int value1 =
        switch (selector) {
          case 1:
            yield 1 + 2;
          default:
            yield 1 + 1;
        };
    @IntVal({2, 3}) int value2 =
        switch (selector) {
          case 1 -> 1 + 2;
          default -> 1 + 1;
        };

    int tmp =
        switch (selector) {
          case 1 -> 1 + 2;
          default -> 1 + 1;
        };
    @IntVal({2, 3}) int value3 = tmp;
  }

  void method1(int selector) {

    @IntVal(3) int value1 =
        // :: error: (assignment)
        switch (selector) {
          case 1:
            yield 1 + 2;
          default:
            yield 1 + 1;
        };

    @IntVal(3) int value2 =
        // :: error: (assignment)
        switch (selector) {
          case 1 -> 1 + 2;
          default -> 1 + 1;
        };
  }

  void method2(int selector, int selector2) {

    @IntVal({2, 3}) int value2 =
        switch (selector) {
          case 1 -> {
            yield 1 + 2;
          }
          default -> {
            yield switch (selector2) {
              case 1:
                {
                  @IntVal(3) int inner =
                      switch (selector) {
                        case 1 -> 1 + 2;
                        default -> 1 + 2;
                      };
                  yield 1 + 2;
                }
              default:
                yield 1 + 1;
            };
          }
        };
  }
}
