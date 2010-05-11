import checkers.interning.quals.*;

import java.util.*;

// The @Interned annotation indicates that much like an enum, all variables
// declared of this type are interned (except the constuctor return value).
// (Perhaps unless otherwise annotated with @Uninterned??  Don't bother to
// implement that yet.)
public class Constants {

  // All but D should be inferred to be @Interned String.
  final String A = "A";
  final String B = "B";
  final String AB = A + B;
  final String AC = A + "C";
  final String D = new String("D");

  void foo() {
    @Interned String is;
    is = A;
    is = B;
    is = AB;
    is = A + B;
    is = AC;
    is = A + "C";
    //:: (type.incompatible)
    is = D;
  }

}
