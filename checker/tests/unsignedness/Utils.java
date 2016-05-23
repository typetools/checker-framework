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