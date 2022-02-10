import org.checkerframework.checker.signedness.qual.Signed;
import org.checkerframework.checker.signedness.qual.UnknownSignedness;
import org.checkerframework.checker.signedness.qual.Unsigned;

public class UnsignedConcat {
  @UnknownSignedness int unknown = -3;
  @Unsigned short unsignedShort = -2;
  @Unsigned int unsignedInt = -2;
  @Signed short signedShort = -2;
  @Signed int signedInt = -2;

  void test1() {
    // :: error: (unsigned.concat)
    String s1 = "" + unsignedShort;
    // :: error: (unsigned.concat)
    String s2 = "" + unsignedInt;
    // :: error: (unsigned.concat)
    String s1b = unsignedShort + "";
    // :: error: (unsigned.concat)
    String s2b = "" + unsignedInt + "";
    String s3 = "" + signedShort;
    String s4 = "" + signedInt;
    // :: error: (unsigned.concat)
    String s5 = "" + unknown;
    String s6 = "" + -1;
  }

  void test2(String s) {
    // :: error: (unsigned.concat)
    s += unsignedShort;
    // :: error: (unsigned.concat)
    s += +unsignedInt;
    s += "" + signedShort;
    s += signedInt;
    // :: error: (unsigned.concat)
    s += unknown;
    s += 9;
  }
}
