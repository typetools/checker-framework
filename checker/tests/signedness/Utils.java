import org.checkerframework.checker.signedness.Signednessutil;
import org.checkerframework.checker.signedness.qual.*;

import java.nio.ByteBuffer;

public class Utils {

    public void getTests(@Unsigned short ushort, @Signed short sshort,
        @Unsigned byte ubyte, @Signed byte sbyte,
        @Unsigned byte[] ubyteArr, @Signed byte[] sbyteArr, ByteBuffer b) {

        //:: error: (assignment.type.incompatible)
        sshort = Signednessutil.getUnsignedShort(b);

        ushort = Signednessutil.getUnsignedShort(b);

        //:: error: (assignment.type.incompatible)
        sbyte = Signednessutil.getUnsigned(b);

        ubyte = Signednessutil.getUnsigned(b);

        //:: error: (argument.type.incompatible)
        Signednessutil.getUnsigned(b, sbyteArr);

        Signednessutil.getUnsigned(b, ubyteArr);
    }

    public void compTests(@Unsigned long ulong, @Signed long slong,
        @Unsigned int uint, @Signed int sint,
        @Unsigned short ushort, @Signed short sshort,
        @Unsigned byte ubyte, @Signed byte sbyte) {

        int res;

        //:: error: (argument.type.incompatible)
        res = Signednessutil.compareUnsigned(slong, slong);

        //:: error: (argument.type.incompatible)
        res = Signednessutil.compareUnsigned(slong, ulong);

        //:: error: (argument.type.incompatible)
        res = Signednessutil.compareUnsigned(ulong, slong);

        res = Signednessutil.compareUnsigned(ulong, ulong);


        //:: error: (argument.type.incompatible)
        res = Signednessutil.compareUnsigned(sint, sint);

        //:: error: (argument.type.incompatible)
        res = Signednessutil.compareUnsigned(sint, uint);

        //:: error: (argument.type.incompatible)
        res = Signednessutil.compareUnsigned(uint, sint);

        res = Signednessutil.compareUnsigned(uint, uint);


        //:: error: (argument.type.incompatible)
        res = Signednessutil.compareUnsigned(sshort, sshort);

        //:: error: (argument.type.incompatible)
        res = Signednessutil.compareUnsigned(sshort, ushort);

        //:: error: (argument.type.incompatible)
        res = Signednessutil.compareUnsigned(ushort, sshort);

        res = Signednessutil.compareUnsigned(ushort, ushort);


        //:: error: (argument.type.incompatible)
        res = Signednessutil.compareUnsigned(sbyte, sbyte);

        //:: error: (argument.type.incompatible)
        res = Signednessutil.compareUnsigned(sbyte, ubyte);

        //:: error: (argument.type.incompatible)
        res = Signednessutil.compareUnsigned(ubyte, sbyte);

        res = Signednessutil.compareUnsigned(ubyte, ubyte);
    }

    public void stringTests(@Unsigned long ulong, @Signed long slong,
        @Unsigned int uint, @Signed int sint,
        @Unsigned short ushort, @Signed short sshort,
        @Unsigned byte ubyte, @Signed byte sbyte) {

        String res;

        //:: error: (argument.type.incompatible)
        res = Signednessutil.toUnsignedString(slong);

        res = Signednessutil.toUnsignedString(ulong);

        //:: error: (argument.type.incompatible)
        res = Signednessutil.toUnsignedString(slong, 10);

        res = Signednessutil.toUnsignedString(ulong, 10);


        //:: error: (argument.type.incompatible)
        res = Signednessutil.toUnsignedString(sint);

        res = Signednessutil.toUnsignedString(uint);

        //:: error: (argument.type.incompatible)
        res = Signednessutil.toUnsignedString(sint, 10);

        res = Signednessutil.toUnsignedString(uint, 10);


        //:: error: (argument.type.incompatible)
        res = Signednessutil.toUnsignedString(sshort);

        res = Signednessutil.toUnsignedString(ushort);

        //:: error: (argument.type.incompatible)
        res = Signednessutil.toUnsignedString(sshort, 10);

        res = Signednessutil.toUnsignedString(ushort, 10);


        //:: error: (argument.type.incompatible)
        res = Signednessutil.toUnsignedString(sbyte);

        res = Signednessutil.toUnsignedString(ubyte);

        //:: error: (argument.type.incompatible)
        res = Signednessutil.toUnsignedString(sbyte, 10);

        res = Signednessutil.toUnsignedString(ubyte, 10);
    }
}
