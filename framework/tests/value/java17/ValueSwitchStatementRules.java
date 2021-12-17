// @below-java17-jdk-skip-test
import org.checkerframework.common.value.qual.IntVal;

public class ValueSwitchStatementRules {
  private int field;

  void method(int selector) {
    field = 300;
    switch (selector) {
      case 1:
        field = 42;
        @IntVal(42) int copyField = field;
      case 2:
        // :: error: (assignment)
        @IntVal(300) int copyField2 = field;
    }

    field = 300;
    switch (selector) {
      case 1 -> {
        field = 42;
        @IntVal(42) int copyField = field;
      }
      case 2 -> {
        @IntVal(300) int copyField = field;
      }
    }
  }
}
