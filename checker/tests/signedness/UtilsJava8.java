import org.checkerframework.checker.signedness.qual.*;

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

        // :: error: (argument.type.incompatible)
        resString = Long.toUnsignedString(slong, sint);

        resString = Long.toUnsignedString(ulong, sint);

        // :: error: (argument.type.incompatible)
        resString = Long.toUnsignedString(slong);

        resString = Long.toUnsignedString(ulong);

        // :: error: (assignment.type.incompatible)
        slong = Long.parseUnsignedLong(s, sint);

        ulong = Long.parseUnsignedLong(s, sint);

        // :: error: (assignment.type.incompatible)
        slong = Long.parseUnsignedLong(s);

        ulong = Long.parseUnsignedLong(s);

        // :: error: (argument.type.incompatible)
        resInt = Long.compareUnsigned(slong, slong);

        // :: error: (argument.type.incompatible)
        resInt = Long.compareUnsigned(slong, ulong);

        // :: error: (argument.type.incompatible)
        resInt = Long.compareUnsigned(ulong, slong);

        resInt = Long.compareUnsigned(ulong, ulong);

        // :: error: (argument.type.incompatible)
        ulong = Long.divideUnsigned(slong, slong);

        // :: error: (argument.type.incompatible)
        ulong = Long.divideUnsigned(slong, ulong);

        // :: error: (argument.type.incompatible)
        ulong = Long.divideUnsigned(ulong, slong);

        // :: error: (assignment.type.incompatible)
        slong = Long.divideUnsigned(ulong, ulong);

        ulong = Long.divideUnsigned(ulong, ulong);

        // :: error: (argument.type.incompatible)
        ulong = Long.remainderUnsigned(slong, slong);

        // :: error: (argument.type.incompatible)
        ulong = Long.remainderUnsigned(slong, ulong);

        // :: error: (argument.type.incompatible)
        ulong = Long.remainderUnsigned(ulong, slong);

        // :: error: (assignment.type.incompatible)
        slong = Long.remainderUnsigned(ulong, ulong);

        ulong = Long.remainderUnsigned(ulong, ulong);

        // :: error: (argument.type.incompatible)
        resString = Integer.toUnsignedString(sint, sint);

        resString = Integer.toUnsignedString(uint, sint);

        // :: error: (argument.type.incompatible)
        resString = Integer.toUnsignedString(sint);

        resString = Integer.toUnsignedString(uint);

        // :: error: (assignment.type.incompatible)
        sint = Integer.parseUnsignedInt(s, sint);

        uint = Integer.parseUnsignedInt(s, sint);

        // :: error: (assignment.type.incompatible)
        sint = Integer.parseUnsignedInt(s);

        uint = Integer.parseUnsignedInt(s);

        // :: error: (argument.type.incompatible)
        resInt = Integer.compareUnsigned(sint, sint);

        // :: error: (argument.type.incompatible)
        resInt = Integer.compareUnsigned(sint, uint);

        // :: error: (argument.type.incompatible)
        resInt = Integer.compareUnsigned(uint, sint);

        resInt = Integer.compareUnsigned(uint, uint);

        // :: error: (argument.type.incompatible)
        resLong = Integer.toUnsignedLong(sint);

        resLong = Integer.toUnsignedLong(uint);

        // :: error: (argument.type.incompatible)
        uint = Integer.divideUnsigned(sint, sint);

        // :: error: (argument.type.incompatible)
        uint = Integer.divideUnsigned(sint, uint);

        // :: error: (argument.type.incompatible)
        uint = Integer.divideUnsigned(uint, sint);

        // :: error: (assignment.type.incompatible)
        sint = Integer.divideUnsigned(uint, uint);

        uint = Integer.divideUnsigned(uint, uint);

        // :: error: (argument.type.incompatible)
        uint = Integer.remainderUnsigned(sint, sint);

        // :: error: (argument.type.incompatible)
        uint = Integer.remainderUnsigned(sint, uint);

        // :: error: (argument.type.incompatible)
        uint = Integer.remainderUnsigned(uint, sint);

        // :: error: (assignment.type.incompatible)
        sint = Integer.remainderUnsigned(uint, uint);

        uint = Integer.remainderUnsigned(uint, uint);
    }
}
