import org.checkerframework.checker.signedness.qual.Signed;
import org.checkerframework.checker.signedness.qual.Unsigned;

public class LocalVarDefaults {

  void methodInt(@Unsigned int unsignedInt, @Signed int signedInt) {
    int local = unsignedInt;
    int local2 = signedInt;
  }

  // :: error: (annotations.on.use) :: error:  (anno.on.irrelevant)
  void methodDouble(@Unsigned double unsigned, @Signed double signed) {
    // :: error: (assignment)
    double local = unsigned;
    double local2 = signed;
  }

  void methodInteger(@Unsigned Integer unsignedInt, @Signed Integer signedInt) {
    Integer local = unsignedInt;
    Integer local2 = signedInt;
  }

  // :: error: (annotations.on.use) :: error:  (anno.on.irrelevant)
  void methodDoubleWrapper(@Unsigned Double unsigned, @Signed Double signed) {
    // :: error: (assignment)
    Double local = unsigned;
    Double local2 = signed;
  }
}
