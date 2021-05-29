import java.nio.ByteBuffer;
import org.checkerframework.checker.signedness.qual.*;
import org.checkerframework.checker.signedness.util.SignednessUtil;

public class Utils {

  public void getTests(
      @Unsigned int uint,
      @Signed int sint,
      @Unsigned short ushort,
      @Signed short sshort,
      @Unsigned byte ubyte,
      @Signed byte sbyte,
      @Unsigned byte[] ubyteArr,
      @Signed byte[] sbyteArr,
      ByteBuffer b) {

    // :: error: (assignment)
    sint = SignednessUtil.getUnsignedInt(b);

    uint = SignednessUtil.getUnsignedInt(b);

    // :: error: (assignment)
    sshort = SignednessUtil.getUnsignedShort(b);

    ushort = SignednessUtil.getUnsignedShort(b);

    // :: error: (assignment)
    sbyte = SignednessUtil.getUnsigned(b);

    ubyte = SignednessUtil.getUnsigned(b);

    // :: error: (argument)
    SignednessUtil.getUnsigned(b, sbyteArr);

    SignednessUtil.getUnsigned(b, ubyteArr);
  }

  public void compTests(
      @Unsigned long ulong,
      @Signed long slong,
      @Unsigned int uint,
      @Signed int sint,
      @Unsigned short ushort,
      @Signed short sshort,
      @Unsigned byte ubyte,
      @Signed byte sbyte) {

    int res;

    // :: error: (argument)
    res = Long.compareUnsigned(slong, slong);

    // :: error: (argument)
    res = Long.compareUnsigned(slong, ulong);

    // :: error: (argument)
    res = Long.compareUnsigned(ulong, slong);

    res = Long.compareUnsigned(ulong, ulong);

    // :: error: (argument)
    res = Integer.compareUnsigned(sint, sint);

    // :: error: (argument)
    res = Integer.compareUnsigned(sint, uint);

    // :: error: (argument)
    res = Integer.compareUnsigned(uint, sint);

    res = Integer.compareUnsigned(uint, uint);

    // :: error: (argument)
    res = SignednessUtil.compareUnsigned(sshort, sshort);

    // :: error: (argument)
    res = SignednessUtil.compareUnsigned(sshort, ushort);

    // :: error: (argument)
    res = SignednessUtil.compareUnsigned(ushort, sshort);

    res = SignednessUtil.compareUnsigned(ushort, ushort);

    // :: error: (argument)
    res = SignednessUtil.compareUnsigned(sbyte, sbyte);

    // :: error: (argument)
    res = SignednessUtil.compareUnsigned(sbyte, ubyte);

    // :: error: (argument)
    res = SignednessUtil.compareUnsigned(ubyte, sbyte);

    res = SignednessUtil.compareUnsigned(ubyte, ubyte);
  }

  public void stringTests(
      @Unsigned long ulong,
      @Signed long slong,
      @Unsigned int uint,
      @Signed int sint,
      @Unsigned short ushort,
      @Signed short sshort,
      @Unsigned byte ubyte,
      @Signed byte sbyte) {

    String res;

    // :: error: (argument)
    res = Long.toUnsignedString(slong);

    res = Long.toUnsignedString(ulong);

    // :: error: (argument)
    res = Long.toUnsignedString(slong, 10);

    res = Long.toUnsignedString(ulong, 10);

    // :: error: (argument)
    res = Integer.toUnsignedString(sint);

    res = Integer.toUnsignedString(uint);

    // :: error: (argument)
    res = Integer.toUnsignedString(sint, 10);

    res = Integer.toUnsignedString(uint, 10);

    // :: error: (argument)
    res = SignednessUtil.toUnsignedString(sshort);

    res = SignednessUtil.toUnsignedString(ushort);

    // :: error: (argument)
    res = SignednessUtil.toUnsignedString(sshort, 10);

    res = SignednessUtil.toUnsignedString(ushort, 10);

    // :: error: (argument)
    res = SignednessUtil.toUnsignedString(sbyte);

    res = SignednessUtil.toUnsignedString(ubyte);

    // :: error: (argument)
    res = SignednessUtil.toUnsignedString(sbyte, 10);

    res = SignednessUtil.toUnsignedString(ubyte, 10);
  }

  public void floatingPointConversionTests(
      @Unsigned long ulong, @Unsigned int uint, @Unsigned short ushort, @Unsigned byte ubyte) {

    float resFloat;

    resFloat = SignednessUtil.toFloat(ubyte);
    resFloat = SignednessUtil.toFloat(ushort);
    resFloat = SignednessUtil.toFloat(uint);
    resFloat = SignednessUtil.toFloat(ulong);

    double resDouble;

    resDouble = SignednessUtil.toDouble(ubyte);
    resDouble = SignednessUtil.toDouble(ushort);
    resDouble = SignednessUtil.toDouble(uint);
    resDouble = SignednessUtil.toDouble(ulong);
  }
}
