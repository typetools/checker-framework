import java.util.Arrays;
import org.checkerframework.checker.index.qual.*;
import org.checkerframework.common.value.qual.IntVal;

public class SearchIndexTests {
  public void test(short[] a, short instant) {
    int i = Arrays.binarySearch(a, instant);
    @SearchIndexFor("a") int z = i;
    // :: error: [assignment]
    @SearchIndexFor("a") int y = 7;
    @LTLengthOf("a") int x = i;
  }

  void test2(int[] a, @SearchIndexFor("#1") int xyz) {
    if (0 > xyz) {
      @NegativeIndexFor("a") int w = xyz;
      @NonNegative int y = ~xyz;
      @LTEqLengthOf("a") int z = ~xyz;
    }
  }

  void test3(int[] a, @SearchIndexFor("#1") int xyz) {
    if (-1 >= xyz) {
      @NegativeIndexFor("a") int w = xyz;
      @NonNegative int y = ~xyz;
      @LTEqLengthOf("a") int z = ~xyz;
    }
  }

  void test4(int[] a, @SearchIndexFor("#1") int xyz) {
    if (xyz < 0) {
      @NegativeIndexFor("a") int w = xyz;
      @NonNegative int y = ~xyz;
      @LTEqLengthOf("a") int z = ~xyz;
    }
  }

  void test5(int[] a, @SearchIndexFor("#1") int xyz) {
    if (xyz <= -1) {
      @NegativeIndexFor("a") int w = xyz;
      @NonNegative int y = ~xyz;
      @LTEqLengthOf("a") int z = ~xyz;
    }
  }

  // Returns 0.
  static @IntVal(0) int zero(int[] ignored) {
    return 0;
  }

  // The right operand reassigns `a`, so the comparison says nothing about the new `a`.
  void otherOperandReassignsArray(int[] a, int[] b, @SearchIndexFor("#1") int xyz) {
    int copy;
    if ((copy = xyz) < zero(a = new int[0])) {
      // :: error: [assignment]
      @NegativeIndexFor("a") int w = copy;
      // :: error: [assignment]
      @NegativeIndexFor("a") int v = xyz;
    }
  }

  // The right operand reassigns `a` but not `b`.
  void otherOperandReassignsOneArray(int[] a, int[] b, @SearchIndexFor({"#1", "#2"}) int xyz) {
    int copy;
    if ((copy = xyz) < zero(a = new int[0])) {
      @NegativeIndexFor("b") int w = copy;
      @NegativeIndexFor("b") int v = xyz;
      // :: error: [assignment]
      @NegativeIndexFor("a") int u = copy;
    }
  }

  void otherOperandDoesNotReassignArray(int[] a, int[] b, @SearchIndexFor("#1") int xyz) {
    int copy;
    if ((copy = xyz) < zero(b = new int[0])) {
      @NegativeIndexFor("a") int w = copy;
      @NegativeIndexFor("a") int v = xyz;
    }
  }

  void subtyping1(
      @SearchIndexFor({"#3", "#4"}) int x, @NegativeIndexFor("#3") int y, int[] a, int[] b) {
    // :: error: [assignment]
    @SearchIndexFor({"a", "b"}) int z = y;
    @SearchIndexFor("a") int w = y;
    @SearchIndexFor("b") int p = x;
    // :: error: [assignment]
    @NegativeIndexFor({"a", "b"}) int q = x;
  }
}
