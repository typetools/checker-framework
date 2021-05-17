import org.checkerframework.checker.index.qual.GTENegativeOne;
import org.checkerframework.checker.index.qual.LTEqLengthOf;
import org.checkerframework.checker.index.qual.LTLengthOf;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.index.qual.PolyLowerBound;
import org.checkerframework.checker.index.qual.PolySameLen;
import org.checkerframework.checker.index.qual.PolyUpperBound;
import org.checkerframework.checker.index.qual.Positive;
import org.checkerframework.checker.index.qual.SameLen;

public class Polymorphic {

  // Identity functions

  @PolyLowerBound int lbc_identity(@PolyLowerBound int a) {
    return a;
  }

  int @PolySameLen [] samelen_identity(int @PolySameLen [] a) {
    int @SameLen("a") [] x = a;
    return a;
  }

  @PolyUpperBound int ubc_identity(@PolyUpperBound int a) {
    return a;
  }

  // SameLen tests
  void samelen_id(int @SameLen("#2") [] a, int[] a2) {
    int[] banana;
    int @SameLen("a2") [] b = samelen_identity(a);
    // :: error: (assignment)
    int @SameLen("banana") [] c = samelen_identity(b);
  }

  // UpperBound tests
  void ubc_id(
      int[] a,
      int[] b,
      @LTLengthOf("#1") int ai,
      @LTEqLengthOf("#1") int al,
      @LTLengthOf({"#1", "#2"}) int abi,
      @LTEqLengthOf({"#1", "#2"}) int abl) {
    int[] c;

    @LTLengthOf("a") int ai1 = ubc_identity(ai);
    // :: error: (assignment)
    @LTLengthOf("b") int ai2 = ubc_identity(ai);

    @LTEqLengthOf("a") int al1 = ubc_identity(al);
    // :: error: (assignment)
    @LTLengthOf("a") int al2 = ubc_identity(al);

    @LTLengthOf({"a", "b"}) int abi1 = ubc_identity(abi);
    // :: error: (assignment)
    @LTLengthOf({"a", "b", "c"}) int abi2 = ubc_identity(abi);

    @LTEqLengthOf({"a", "b"}) int abl1 = ubc_identity(abl);
    // :: error: (assignment)
    @LTEqLengthOf({"a", "b", "c"}) int abl2 = ubc_identity(abl);
  }

  // LowerBound tests
  void lbc_id(@NonNegative int n, @Positive int p, @GTENegativeOne int g) {
    @NonNegative int an = lbc_identity(n);
    // :: error: (assignment)
    @Positive int bn = lbc_identity(n);

    @GTENegativeOne int ag = lbc_identity(g);
    // :: error: (assignment)
    @NonNegative int bg = lbc_identity(g);

    @Positive int ap = lbc_identity(p);
  }
}
