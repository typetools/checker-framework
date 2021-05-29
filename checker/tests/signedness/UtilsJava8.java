import org.checkerframework.checker.signedness.qual.Signed;
import org.checkerframework.checker.signedness.qual.Unsigned;

// Test Java 8 unsigned utils
public class UtilsJava8 {

  public void annotatedJDKTests(
      @Unsigned long ulong,
      @Signed long slong,
      @Unsigned int uint,
      @Signed int sint,
      char[] buf,
      String s) {

    String resString;
    int resInt;
    long resLong;

    // :: error: (argument)
    resString = Long.toUnsignedString(slong, 10);

    resString = Long.toUnsignedString(ulong, 10);

    // :: error: (argument)
    resString = Long.toUnsignedString(slong);

    resString = Long.toUnsignedString(ulong);

    // :: error: (assignment)
    slong = Long.parseUnsignedLong(s, 10);

    ulong = Long.parseUnsignedLong(s, 10);

    // :: error: (assignment)
    slong = Long.parseUnsignedLong(s);

    ulong = Long.parseUnsignedLong(s);

    // :: error: (argument)
    resInt = Long.compareUnsigned(slong, slong);

    // :: error: (argument)
    resInt = Long.compareUnsigned(slong, ulong);

    // :: error: (argument)
    resInt = Long.compareUnsigned(ulong, slong);

    resInt = Long.compareUnsigned(ulong, ulong);

    // :: error: (argument)
    ulong = Long.divideUnsigned(slong, slong);

    // :: error: (argument)
    ulong = Long.divideUnsigned(slong, ulong);

    // :: error: (argument)
    ulong = Long.divideUnsigned(ulong, slong);

    // :: error: (assignment)
    slong = Long.divideUnsigned(ulong, ulong);

    ulong = Long.divideUnsigned(ulong, ulong);

    // :: error: (argument)
    ulong = Long.remainderUnsigned(slong, slong);

    // :: error: (argument)
    ulong = Long.remainderUnsigned(slong, ulong);

    // :: error: (argument)
    ulong = Long.remainderUnsigned(ulong, slong);

    // :: error: (assignment)
    slong = Long.remainderUnsigned(ulong, ulong);

    ulong = Long.remainderUnsigned(ulong, ulong);

    // :: error: (argument)
    resString = Integer.toUnsignedString(sint, 10);

    resString = Integer.toUnsignedString(uint, 10);

    // :: error: (argument)
    resString = Integer.toUnsignedString(sint);

    resString = Integer.toUnsignedString(uint);

    // :: error: (assignment)
    sint = Integer.parseUnsignedInt(s, 10);

    uint = Integer.parseUnsignedInt(s, 10);

    // :: error: (assignment)
    sint = Integer.parseUnsignedInt(s);

    uint = Integer.parseUnsignedInt(s);

    // :: error: (argument)
    resInt = Integer.compareUnsigned(sint, sint);

    // :: error: (argument)
    resInt = Integer.compareUnsigned(sint, uint);

    // :: error: (argument)
    resInt = Integer.compareUnsigned(uint, sint);

    resInt = Integer.compareUnsigned(uint, uint);

    resLong = Integer.toUnsignedLong(sint);

    // :: error: (argument)
    ulong = Integer.toUnsignedLong(uint);

    // :: error: (argument)
    uint = Integer.divideUnsigned(sint, sint);

    // :: error: (argument)
    uint = Integer.divideUnsigned(sint, uint);

    // :: error: (argument)
    uint = Integer.divideUnsigned(uint, sint);

    // :: error: (assignment)
    sint = Integer.divideUnsigned(uint, uint);

    uint = Integer.divideUnsigned(uint, uint);

    // :: error: (argument)
    uint = Integer.remainderUnsigned(sint, sint);

    // :: error: (argument)
    uint = Integer.remainderUnsigned(sint, uint);

    // :: error: (argument)
    uint = Integer.remainderUnsigned(uint, sint);

    // :: error: (assignment)
    sint = Integer.remainderUnsigned(uint, uint);

    uint = Integer.remainderUnsigned(uint, uint);
  }
}
