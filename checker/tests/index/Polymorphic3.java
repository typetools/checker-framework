import org.checkerframework.checker.index.qual.*;

public class Polymorphic3 {

  // Identity functions

  @PolyIndex int identity(@PolyIndex int a) {
    return a;
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

    @LTLengthOf("a") int ai1 = identity(ai);
    // :: error: (assignment)
    @LTLengthOf("b") int ai2 = identity(ai);

    @LTEqLengthOf("a") int al1 = identity(al);
    // :: error: (assignment)
    @LTLengthOf("a") int al2 = identity(al);

    @LTLengthOf({"a", "b"}) int abi1 = identity(abi);
    // :: error: (assignment)
    @LTLengthOf({"a", "b", "c"}) int abi2 = identity(abi);

    @LTEqLengthOf({"a", "b"}) int abl1 = identity(abl);
    // :: error: (assignment)
    @LTEqLengthOf({"a", "b", "c"}) int abl2 = identity(abl);
  }

  // LowerBound tests
  void lbc_id(@NonNegative int n, @Positive int p, @GTENegativeOne int g) {
    @NonNegative int an = identity(n);
    // :: error: (assignment)
    @Positive int bn = identity(n);

    @GTENegativeOne int ag = identity(g);
    // :: error: (assignment)
    @NonNegative int bg = identity(g);

    @Positive int ap = identity(p);
  }
}
