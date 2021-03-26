package index;

import org.checkerframework.checker.index.qual.GTENegativeOne;
import org.checkerframework.checker.index.qual.IndexFor;
import org.checkerframework.checker.index.qual.SameLenBottom;
import org.checkerframework.checker.index.qual.UpperBoundBottom;
import org.checkerframework.common.value.qual.BottomVal;

/** This class tests that errors are issued in order of postion. */
public class ErrorOrders {

  void test2(int i, int[] a) {
    a[i] = 2;
  }

  int test4(
      @GTENegativeOne @UpperBoundBottom int p1,
      @UpperBoundBottom @GTENegativeOne int p2,
      int @BottomVal [] p3,
      int @SameLenBottom [] p4,
      @BottomVal int p5) {

    @IndexFor("p2") int z = 0;
    @IndexFor("This isn't an expression") int x = z;
    return x;
  }

  void useTest4(int p1, int p2, int[] p3, int[] p4, int p5) {
    test4(p1, test4(p1, p2, p3, p4, p5), p3, p4, p5);
  }

  class InnerClass {
    @IndexFor("This isn't an expression") int x = 0;

    void test2(int i, int[] a) {
      a[i] = 2;
    }
  }
}

class InSameCompilationUnit {
  @IndexFor("This isn't an expression") int x = 0;

  void test2(int i, int[] a) {
    a[i] = 2;
  }

  int test4(
      @GTENegativeOne @UpperBoundBottom int p1,
      @UpperBoundBottom @GTENegativeOne int p2,
      int @BottomVal [] p3,
      int @SameLenBottom [] p4,
      @BottomVal int p5) {

    @IndexFor("p2") int z = 0;
    @IndexFor("This isn't an expression") int x = z;
    return x;
  }

  void useTest4(int p1, int p2, int[] p3, int[] p4, int p5) {
    test4(p1, test4(p1, p2, p3, p4, p5), p3, p4, p5);
  }
}
