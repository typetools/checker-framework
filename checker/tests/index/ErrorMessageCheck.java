import org.checkerframework.checker.index.qual.NonNegative;

public class ErrorMessageCheck {
  @NonNegative int size;
  int[] vDown = new int[size];

  void method3(@NonNegative int size, @NonNegative int value) {
    this.size = size;
    this.vDown = new int[this.size];
    // :: error: (array.access.unsafe.high)
    vDown[1 + value] = 10;
  }
}
