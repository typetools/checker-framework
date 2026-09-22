// Test that the canonical form of a @MinLen annotation does not depend on the type that the
// @MinLen annotation is written on.  @MinLen(n) is @ArrayLenRange(from = n, to =
// Integer.MAX_VALUE) on every type, because the length of a sequence is always an int.

import org.checkerframework.common.value.qual.ArrayLenRange;
import org.checkerframework.common.value.qual.MinLen;

public class MinLenCanonicalization {

  /** A user-defined sequence type. */
  static class Seq {}

  void userDefinedType(@MinLen(3) Seq a) {
    @MinLen(3) Seq same = a;
    @MinLen(2) Seq weaker = a;
    @ArrayLenRange(from = 3, to = Integer.MAX_VALUE) Seq range = a;
    // :: error: [assignment]
    @MinLen(4) Seq stronger = a;
  }

  void charSequence(@MinLen(10) CharSequence cs) {
    @MinLen(10) CharSequence same = cs;
    @MinLen(9) CharSequence weaker = cs;
    @ArrayLenRange(from = 10, to = Integer.MAX_VALUE) CharSequence range = cs;
    // :: error: [assignment]
    @MinLen(11) CharSequence stronger = cs;
  }

  void charSequenceLiteral() {
    @MinLen(10) CharSequence exact = "0123456789";
    @MinLen(4) CharSequence shorter = "0123456789";
    // :: error: [assignment]
    @MinLen(11) CharSequence longer = "0123456789";
  }

  void arrayType(int @MinLen(3) [] a) {
    int @ArrayLenRange(from = 3, to = Integer.MAX_VALUE) [] range = a;
    // :: error: [assignment]
    int @MinLen(4) [] stronger = a;
  }

  void stringType(@MinLen(3) String s) {
    @ArrayLenRange(from = 3, to = Integer.MAX_VALUE) String range = s;
    // :: error: [assignment]
    @MinLen(4) String stronger = s;
  }

  // A @MinLen annotation on a numeric type is meaningless, and the Value Checker does not reject
  // it.  Canonicalization still does not consult the type: the maximum is Integer.MAX_VALUE, not
  // the maximum value of the numeric type.

  void longType(@MinLen(3) long l) {
    @ArrayLenRange(from = 3, to = Integer.MAX_VALUE) long range = l;
    // :: error: [assignment]
    @MinLen(4) long stronger = l;
  }

  void byteType(@MinLen(3) byte b) {
    @ArrayLenRange(from = 3, to = Integer.MAX_VALUE) byte range = b;
    // :: error: [assignment]
    @MinLen(4) byte stronger = b;
  }
}
