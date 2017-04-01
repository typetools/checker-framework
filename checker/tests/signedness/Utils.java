import java.nio.ByteBuffer;
import org.checkerframework.checker.signedness.SignednessUtil;
import org.checkerframework.checker.signedness.qual.*;

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

        //:: error: (assignment.type.incompatible)
        sint = SignednessUtil.getUnsignedInt(b);

        uint = SignednessUtil.getUnsignedInt(b);

        //:: error: (assignment.type.incompatible)
        sshort = SignednessUtil.getUnsignedShort(b);

        ushort = SignednessUtil.getUnsignedShort(b);

        //:: error: (assignment.type.incompatible)
        sbyte = SignednessUtil.getUnsigned(b);

        ubyte = SignednessUtil.getUnsigned(b);

        //:: error: (argument.type.incompatible)
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

        //:: error: (argument.type.incompatible)
        res = SignednessUtil.compareUnsigned(slong, slong);

        //:: error: (argument.type.incompatible)
        res = SignednessUtil.compareUnsigned(slong, ulong);

        //:: error: (argument.type.incompatible)
        res = SignednessUtil.compareUnsigned(ulong, slong);

        res = SignednessUtil.compareUnsigned(ulong, ulong);

        //:: error: (argument.type.incompatible)
        res = SignednessUtil.compareUnsigned(sint, sint);

        //:: error: (argument.type.incompatible)
        res = SignednessUtil.compareUnsigned(sint, uint);

        //:: error: (argument.type.incompatible)
        res = SignednessUtil.compareUnsigned(uint, sint);

        res = SignednessUtil.compareUnsigned(uint, uint);

        //:: error: (argument.type.incompatible)
        res = SignednessUtil.compareUnsigned(sshort, sshort);

        //:: error: (argument.type.incompatible)
        res = SignednessUtil.compareUnsigned(sshort, ushort);

        //:: error: (argument.type.incompatible)
        res = SignednessUtil.compareUnsigned(ushort, sshort);

        res = SignednessUtil.compareUnsigned(ushort, ushort);

        //:: error: (argument.type.incompatible)
        res = SignednessUtil.compareUnsigned(sbyte, sbyte);

        //:: error: (argument.type.incompatible)
        res = SignednessUtil.compareUnsigned(sbyte, ubyte);

        //:: error: (argument.type.incompatible)
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

        //:: error: (argument.type.incompatible)
        res = SignednessUtil.toUnsignedString(slong);

        res = SignednessUtil.toUnsignedString(ulong);

        //:: error: (argument.type.incompatible)
        res = SignednessUtil.toUnsignedString(slong, 10);

        res = SignednessUtil.toUnsignedString(ulong, 10);

        //:: error: (argument.type.incompatible)
        res = SignednessUtil.toUnsignedString(sint);

        res = SignednessUtil.toUnsignedString(uint);

        //:: error: (argument.type.incompatible)
        res = SignednessUtil.toUnsignedString(sint, 10);

        res = SignednessUtil.toUnsignedString(uint, 10);

        //:: error: (argument.type.incompatible)
        res = SignednessUtil.toUnsignedString(sshort);

        res = SignednessUtil.toUnsignedString(ushort);

        //:: error: (argument.type.incompatible)
        res = SignednessUtil.toUnsignedString(sshort, 10);

        res = SignednessUtil.toUnsignedString(ushort, 10);

        //:: error: (argument.type.incompatible)
        res = SignednessUtil.toUnsignedString(sbyte);

        res = SignednessUtil.toUnsignedString(ubyte);

        //:: error: (argument.type.incompatible)
        res = SignednessUtil.toUnsignedString(sbyte, 10);

        res = SignednessUtil.toUnsignedString(ubyte, 10);
    }
}
