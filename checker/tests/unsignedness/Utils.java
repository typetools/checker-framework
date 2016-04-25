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
    
    public void compTests(@Unsigned int uint, @Signed int sint,
        @Unsigned short ushort, @Signed short sshort,
        @Unsigned byte ubyte, @Signed byte sbyte) {
          
        int res;
        
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
}