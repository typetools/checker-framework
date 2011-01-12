import checkers.interning.quals.*;

import java.util.*;

public class Constants {

  // All but D should be inferred to be @Interned String.
  final String A = "A";
  final String B = "B";
  final String AB = A + B;
  final String AC = A + "C";
  final String D = new String("D");
  final @Interned String E = new String("E").intern();

  void foo() {
    @Interned String is;
    is = A;
    is = B;
    is = AB;
    is = A + B;
    is = AC;
    is = A + "C";
    is = A + B + "C";
    //:: (assignment.type.incompatible)
    is = D;
    //:: (assignment.type.incompatible)
    is = A + E;
    //:: (assignment.type.incompatible)
    is = is + is;
    is = Constants2.E;
  }

}

class Constants2 {
  public static final String E = "e";
}
