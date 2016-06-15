import org.checkerframework.checker.unsignedness.UnsignednessUtil;
import org.checkerframework.checker.unsignedness.qual.*;

import java.nio.ByteBuffer;

public class Utils {

    public void getTests(@Unsigned short ushort, @Signed short sshort,
        @Unsigned byte ubyte, @Signed byte sbyte,
        @Unsigned byte[] ubyteArr, @Signed byte[] sbyteArr, ByteBuffer b) {

        //:: error: (assignment.type.incompatible)
        sshort = UnsignednessUtil.getUnsignedShort(b);

        ushort = UnsignednessUtil.getUnsignedShort(b);

        //:: error: (assignment.type.incompatible)
        sbyte = UnsignednessUtil.getUnsigned(b);

        ubyte = UnsignednessUtil.getUnsigned(b);

        //:: error: (argument.type.incompatible)
        UnsignednessUtil.getUnsigned(b, sbyteArr);

        UnsignednessUtil.getUnsigned(b, ubyteArr);
    }

    public void compTests(@Unsigned long ulong, @Signed long slong,
        @Unsigned int uint, @Signed int sint,
        @Unsigned short ushort, @Signed short sshort,
        @Unsigned byte ubyte, @Signed byte sbyte) {

        int res;

        //:: error: (argument.type.incompatible)
        res = UnsignednessUtil.compareUnsigned(slong, slong);

        //:: error: (argument.type.incompatible)
        res = UnsignednessUtil.compareUnsigned(slong, ulong);

        //:: error: (argument.type.incompatible)
        res = UnsignednessUtil.compareUnsigned(ulong, slong);

        res = UnsignednessUtil.compareUnsigned(ulong, ulong);


        //:: error: (argument.type.incompatible)
        res = UnsignednessUtil.compareUnsigned(sint, sint);

        //:: error: (argument.type.incompatible)
        res = UnsignednessUtil.compareUnsigned(sint, uint);

        //:: error: (argument.type.incompatible)
        res = UnsignednessUtil.compareUnsigned(uint, sint);

        res = UnsignednessUtil.compareUnsigned(uint, uint);


        //:: error: (argument.type.incompatible)
        res = UnsignednessUtil.compareUnsigned(sshort, sshort);

        //:: error: (argument.type.incompatible)
        res = UnsignednessUtil.compareUnsigned(sshort, ushort);

        //:: error: (argument.type.incompatible)
        res = UnsignednessUtil.compareUnsigned(ushort, sshort);

        res = UnsignednessUtil.compareUnsigned(ushort, ushort);


        //:: error: (argument.type.incompatible)
        res = UnsignednessUtil.compareUnsigned(sbyte, sbyte);

        //:: error: (argument.type.incompatible)
        res = UnsignednessUtil.compareUnsigned(sbyte, ubyte);

        //:: error: (argument.type.incompatible)
        res = UnsignednessUtil.compareUnsigned(ubyte, sbyte);

        res = UnsignednessUtil.compareUnsigned(ubyte, ubyte);
    }

    public void stringTests(@Unsigned long ulong, @Signed long slong,
        @Unsigned int uint, @Signed int sint,
        @Unsigned short ushort, @Signed short sshort,
        @Unsigned byte ubyte, @Signed byte sbyte) {

        String res;

        //:: error: (argument.type.incompatible)
        res = UnsignednessUtil.toUnsignedString(slong);

        res = UnsignednessUtil.toUnsignedString(ulong);

        //:: error: (argument.type.incompatible)
        res = UnsignednessUtil.toUnsignedString(slong, 10);

        res = UnsignednessUtil.toUnsignedString(ulong, 10);


        //:: error: (argument.type.incompatible)
        res = UnsignednessUtil.toUnsignedString(sint);

        res = UnsignednessUtil.toUnsignedString(uint);

        //:: error: (argument.type.incompatible)
        res = UnsignednessUtil.toUnsignedString(sint, 10);

        res = UnsignednessUtil.toUnsignedString(uint, 10);


        //:: error: (argument.type.incompatible)
        res = UnsignednessUtil.toUnsignedString(sshort);

        res = UnsignednessUtil.toUnsignedString(ushort);

        //:: error: (argument.type.incompatible)
        res = UnsignednessUtil.toUnsignedString(sshort, 10);

        res = UnsignednessUtil.toUnsignedString(ushort, 10);


        //:: error: (argument.type.incompatible)
        res = UnsignednessUtil.toUnsignedString(sbyte);

        res = UnsignednessUtil.toUnsignedString(ubyte);

        //:: error: (argument.type.incompatible)
        res = UnsignednessUtil.toUnsignedString(sbyte, 10);

        res = UnsignednessUtil.toUnsignedString(ubyte, 10);
    }
}
