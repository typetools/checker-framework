import org.checkerframework.common.value.qual.IntVal;

public class ManualConstructor {

  @IntVal(1) int x;

  @IntVal(2) int y;

  int z;

  // :: error: (contracts.postcondition.not.satisfied)
  ManualConstructor() {
    x = 1;
  }

  // :: error: (contracts.postcondition.not.satisfied)
  ManualConstructor(boolean ignore) {
    x = 1;
    z = 3;
  }

  ManualConstructor(float ignore) {
    x = 1;
    y = 2;
  }

  ManualConstructor(double ignore) {
    x = 1;
    y = 2;
    z = 3;
  }
}
