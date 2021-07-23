import org.checkerframework.checker.interning.qual.Interned;

public class ConstantsInterning {

  // All but D should be inferred to be @Interned String.
  final String A = "A";
  final String B = "B";
  final String AB = A + B;
  final String AC = A + "C";
  final String D = new String("D");
  final @Interned String E = new String("E").intern();
  final Object F = "F";

  void foo() {
    @Interned String is;
    is = A;
    is = B;
    is = AB;
    is = A + B;
    is = AC;
    is = A + "C";
    is = A + B + "C";
    // :: error: (assignment)
    is = D;
    // :: error: (assignment)
    is = A + E;
    // :: error: (assignment)
    is = is + is;
    // :: error: (assignment)
    is = Constants2.E;
    is = (String) F;
  }
}

class Constants2 {
  public static final String E = "e";
}
