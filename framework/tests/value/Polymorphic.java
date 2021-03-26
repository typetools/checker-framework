import org.checkerframework.common.value.qual.*;

public class Polymorphic {

  // Identity functions

  int @PolyValue [] identity_array(int @PolyValue [] a) {
    return a;
  }

  @PolyValue int identity_int(@PolyValue int a) {
    return a;
  }

  void minlen_id(int @MinLen(5) [] a) {
    int @MinLen(5) [] b = identity_array(a);
    // :: error: (assignment.type.incompatible)
    int @MinLen(6) [] c = identity_array(b);
  }

  void use(int @ArrayLenRange(from = 5, to = 25) [] a) {
    int @ArrayLenRange(from = 5, to = 25) [] b = identity_array(a);
    // :: error: (assignment.type.incompatible)
    int @ArrayLenRange(from = 1, to = 13) [] c = identity_array(a);
  }

  void use2(@IntRange(from = 5, to = 25) int a) {
    @IntRange(from = 5, to = 25) int b = identity_int(a);
    // :: error: (assignment.type.incompatible)
    @IntVal(3) int x = identity_int(a);
  }
}
