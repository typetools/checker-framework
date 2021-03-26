import org.checkerframework.checker.index.qual.LTLengthOf;

public class ConstantOffsets {
  void method1(int[] a, int offset, @LTLengthOf(value = "#1", offset = "-#2 - 1") int x) {}

  void test1() {
    int offset = -4;
    int x = 4;
    int[] f1 = new int[x - offset];
    method1(f1, offset, x);
  }

  void method2(int[] a, int offset, @LTLengthOf(value = "#1", offset = "#2 - 1") int x) {}

  void test2() {
    int offset = 4;
    int x = 4;
    int[] f1 = new int[x + offset];
    method2(f1, offset, x);
  }
}
