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
        res = UnsignednessUtil.compareUnsignedLongs(slong, slong);    
        
        //:: error: (argument.type.incompatible)
        res = UnsignednessUtil.compareUnsignedLongs(slong, ulong);
        
        //:: error: (argument.type.incompatible)
        res = UnsignednessUtil.compareUnsignedLongs(ulong, slong);

        res = UnsignednessUtil.compareUnsignedLongs(ulong, ulong);
        
        
        //:: error: (argument.type.incompatible)
        res = UnsignednessUtil.compareUnsignedInts(sint, sint);    
        
        //:: error: (argument.type.incompatible)
        res = UnsignednessUtil.compareUnsignedInts(sint, uint);
        
        //:: error: (argument.type.incompatible)
        res = UnsignednessUtil.compareUnsignedInts(uint, sint);

        res = UnsignednessUtil.compareUnsignedInts(uint, uint);
        
        
        //:: error: (argument.type.incompatible)
        res = UnsignednessUtil.compareUnsignedShorts(sshort, sshort);    
        
        //:: error: (argument.type.incompatible)
        res = UnsignednessUtil.compareUnsignedShorts(sshort, ushort);
        
        //:: error: (argument.type.incompatible)
        res = UnsignednessUtil.compareUnsignedShorts(ushort, sshort);

        res = UnsignednessUtil.compareUnsignedShorts(ushort, ushort);
        
        
        //:: error: (argument.type.incompatible)
        res = UnsignednessUtil.compareUnsignedBytes(sbyte, sbyte);    
        
        //:: error: (argument.type.incompatible)
        res = UnsignednessUtil.compareUnsignedBytes(sbyte, ubyte);
        
        //:: error: (argument.type.incompatible)
        res = UnsignednessUtil.compareUnsignedBytes(ubyte, sbyte);

        res = UnsignednessUtil.compareUnsignedBytes(ubyte, ubyte);
    }
    
    public void annotatedJDKTests(@Unsigned long ulong, @Signed long slong,
        @Unsigned int uint, @Signed int sint,
        char[] buf, String s) {
        
        String resString;
        int resInt;
        long resLong;
        
        //:: error: (argument.type.incompatible)
        resString = Long.toUnsignedString(slong, sint);
        
        resString = Long.toUnsignedString(ulong, sint);
        
        
        //:: error: (argument.type.incompatible)
        resString = Long.toUnsignedString(slong);
        
        resString = Long.toUnsignedString(ulong);
        
        
        //:: error: (assignment.type.incompatible)
        slong = Long.parseUnsignedLong(s, sint);
        
        ulong = Long.parseUnsignedLong(s, sint);
        
        
        //:: error: (assignment.type.incompatible)
        slong = Long.parseUnsignedLong(s);
        
        ulong = Long.parseUnsignedLong(s);
        
        
        //:: error: (argument.type.incompatible)
        resInt = Long.compareUnsigned(slong, slong);
        
        //:: error: (argument.type.incompatible)
        resInt = Long.compareUnsigned(slong, ulong);
        
        //:: error: (argument.type.incompatible)
        resInt = Long.compareUnsigned(ulong, slong);
        
        resInt = Long.compareUnsigned(ulong, ulong);
        
        
        //:: error: (argument.type.incompatible)
        ulong = Long.divideUnsigned(slong, slong);
        
        //:: error: (argument.type.incompatible)
        ulong = Long.divideUnsigned(slong, ulong);
        
        //:: error: (argument.type.incompatible)
        ulong = Long.divideUnsigned(ulong, slong);

        //:: error: (assignment.type.incompatible)
        slong = Long.divideUnsigned(ulong, ulong);

        ulong = Long.divideUnsigned(ulong, ulong);
        
        
        //:: error: (argument.type.incompatible)
        ulong = Long.remainderUnsigned(slong, slong);
        
        //:: error: (argument.type.incompatible)
        ulong = Long.remainderUnsigned(slong, ulong);
        
        //:: error: (argument.type.incompatible)
        ulong = Long.remainderUnsigned(ulong, slong);

        //:: error: (assignment.type.incompatible)
        slong = Long.remainderUnsigned(ulong, ulong);

        ulong = Long.remainderUnsigned(ulong, ulong);
        
        
        
        //:: error: (argument.type.incompatible)
        resString = Integer.toUnsignedString(sint, sint);
        
        resString = Integer.toUnsignedString(uint, sint);
        
        
        //:: error: (argument.type.incompatible)
        resString = Integer.toUnsignedString(sint);
        
        resString = Integer.toUnsignedString(uint);
        
        
        //:: error: (assignment.type.incompatible)
        sint = Integer.parseUnsignedInt(s, sint);
        
        uint = Integer.parseUnsignedInt(s, sint);
        
        
        //:: error: (assignment.type.incompatible)
        sint = Integer.parseUnsignedInt(s);
        
        uint = Integer.parseUnsignedInt(s);
        
        
        //:: error: (argument.type.incompatible)
        resInt = Integer.compareUnsigned(sint, sint);
        
        //:: error: (argument.type.incompatible)
        resInt = Integer.compareUnsigned(sint, uint);
        
        //:: error: (argument.type.incompatible)
        resInt = Integer.compareUnsigned(uint, sint);
        
        resInt = Integer.compareUnsigned(uint, uint);
        
        
        //:: error: (argument.type.incompatible)
        resLong = Integer.toUnsignedLong(sint);
        
        resLong = Integer.toUnsignedLong(uint);
        
        
        //:: error: (argument.type.incompatible)
        uint = Integer.divideUnsigned(sint, sint);
        
        //:: error: (argument.type.incompatible)
        uint = Integer.divideUnsigned(sint, uint);
        
        //:: error: (argument.type.incompatible)
        uint = Integer.divideUnsigned(uint, sint);

        //:: error: (assignment.type.incompatible)
        sint = Integer.divideUnsigned(uint, uint);

        uint = Integer.divideUnsigned(uint, uint);
        
        
        //:: error: (argument.type.incompatible)
        uint = Integer.remainderUnsigned(sint, sint);
        
        //:: error: (argument.type.incompatible)
        uint = Integer.remainderUnsigned(sint, uint);
        
        //:: error: (argument.type.incompatible)
        uint = Integer.remainderUnsigned(uint, sint);

        //:: error: (assignment.type.incompatible)
        sint = Integer.remainderUnsigned(uint, uint);

        uint = Integer.remainderUnsigned(uint, uint);
    }
}