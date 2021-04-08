import org.checkerframework.checker.index.qual.LTEqLengthOf;

public class ArrayLength {
  void test() {
    int[] arr = {1, 2, 3};
    @LTEqLengthOf({"arr"}) int a = arr.length;
  }
}
