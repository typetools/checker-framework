import org.checkerframework.checker.index.qual.*;
import org.checkerframework.common.value.qual.*;

public class ParserOffsetTest {

  public void subtraction1(String[] a, @IndexFor("#1") int i) {
    int length = a.length;
    if (i >= length - 1 || a[i + 1] == null) {
      // body is irrelevant
    }
  }

  public void addition1(String[] a, @IndexFor("#1") int i) {
    int length = a.length;
    if ((i + 1) >= length || a[i + 1] == null) {
      // body is irrelevant
    }
  }

  public void subtraction2(String[] a, @IndexFor("#1") int i) {
    if (i < a.length - 1) {
      @IndexFor("a") int j = i + 1;
    }
  }

  public void addition2(String[] a, @IndexFor("#1") int i) {
    if ((i + 1) < a.length) {
      @IndexFor("a") int j = i + 1;
    }
  }

  public void addition3(String[] a, @IndexFor("#1") int i) {
    if ((i + 5) < a.length) {
      @IndexFor("a") int j = i + 5;
    }
  }

  @SuppressWarnings("lowerbound")
  public void subtraction3(String[] a, @NonNegative int k) {
    if (k - 5 < a.length) {
      String s = a[k - 5];
      @IndexFor("a") int j = k - 5;
    }
  }

  @SuppressWarnings("lowerbound")
  public void subtraction4(String[] a, @IndexFor("#1") int i) {
    if (1 - i < a.length) {
      // The error on this assignment is a false positive.
      // :: error: (assignment)
      @IndexFor("a") int j = 1 - i;

      // :: error: (assignment)
      @LTLengthOf(value = "a", offset = "1") int k = i;
    }
  }

  @SuppressWarnings("lowerbound")
  public void subtraction5(String[] a, int i) {
    if (1 - i < a.length) {
      // :: error: (assignment)
      @IndexFor("a") int j = i;
    }
  }

  @SuppressWarnings("lowerbound")
  public void subtraction6(String[] a, int i, int j) {
    if (i - j < a.length - 1) {
      @IndexFor("a") int k = i - j;
      // :: error: (assignment)
      @IndexFor("a") int k1 = i;
    }
  }

  public void multiplication1(String[] a, int i, @Positive int j) {
    if ((i * j) < (a.length + j)) {
      // :: error: (assignment)
      @IndexFor("a") int k = i;
      // :: error: (assignment)
      @IndexFor("a") int k1 = j;
    }
  }

  public void multiplication2(String @ArrayLen(5) [] a, @IntVal(-2) int i, @IntVal(20) int j) {
    if ((i * j) < (a.length - 20)) {
      @LTLengthOf("a") int k1 = i;
      // :: error: (assignment)
      @LTLengthOf(value = "a", offset = "20") int k2 = i;
      // :: error: (assignment)
      @LTLengthOf("a") int k3 = j;
    }
  }
}
