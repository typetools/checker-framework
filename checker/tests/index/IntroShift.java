import org.checkerframework.checker.index.qual.NonNegative;

public class IntroShift {
  void test() {
    @NonNegative int a = 1 >> 1;
    // :: error: (assignment)
    @NonNegative int b = -1 >> 0;
  }
}
