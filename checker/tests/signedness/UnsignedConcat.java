import org.checkerframework.checker.signedness.qual.Signed;
import org.checkerframework.checker.signedness.qual.UnknownSignedness;
import org.checkerframework.checker.signedness.qual.Unsigned;

public class UnsignedConcat {
  @UnknownSignedness int unknownInt = -3;
  @Unsigned short unsignedShort = -2;
  @Unsigned int unsignedInt = -2;
  @Signed short signedShort = -2;
  @Signed int signedInt = -2;

  void test1(char c, Character charObj) {
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
    String s5 = "" + unknownInt;
    String s6 = "" + -1;

    String s7 = "" + c;
    String s8 = "" + charObj;
  }

  void test2(String s, char c, Character charObj) {
    // :: error: (unsigned.concat)
    // :: error: (compound.assignment)
    s += unsignedShort;
    // :: error: (unsigned.concat)
    // :: error: (compound.assignment)
    s += +unsignedInt;
    s += "" + signedShort;
    s += signedInt;
    // :: error: (unsigned.concat)
    // :: error: (compound.assignment)
    s += unknownInt;
    s += 9;
    s += c;
    s += charObj;
  }
}
