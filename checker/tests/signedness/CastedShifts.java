import org.checkerframework.checker.signedness.qual.*;

public class CastedShifts {

    public void CastedIntShifts(@Unsigned int unsigned, @Signed int signed) {
        // Cast to byte.
        @UnknownSignedness byte byteRes;

        // Shifting right by 23, the introduced bits are cast away
        byteRes = (@Unsigned byte) (unsigned >>> 23);
        byteRes = (@Unsigned byte) (unsigned >> 23);
        byteRes = (@Signed byte) (signed >>> 23);
        byteRes = (@Signed byte) (signed >> 23);

        // Shifting right by 24, the introduced bits are still cast away.
        byteRes = (@Unsigned byte) (unsigned >>> 24);
        byteRes = (@Unsigned byte) (unsigned >> 24);
        byteRes = (@Signed byte) (signed >>> 24);
        byteRes = (@Signed byte) (signed >> 24);

        // Shifting right by 25, now the MSB matters.
        byteRes = (@Unsigned byte) (unsigned >>> 25);

        //:: error: (shift.signed)
        byteRes = (@Unsigned byte) (unsigned >> 25);

        //:: error: (shift.unsigned)
        byteRes = (@Signed byte) (signed >>> 25);
        byteRes = (@Signed byte) (signed >> 25);

        // Shifting right by zero should behave as assignment
        byteRes = (@Unsigned byte) (unsigned >>> 0);
        byteRes = (@Unsigned byte) (unsigned >> 0);
        byteRes = (@Signed byte) (signed >>> 0);
        byteRes = (@Signed byte) (signed >> 0);

        // Cast to char.
        @UnknownSignedness char charRes;

        // Shifting right by 23, the introduced bits are cast away
        charRes = (@Unsigned char) (unsigned >>> 23);
        charRes = (@Unsigned char) (unsigned >> 23);
        charRes = (@Signed char) (signed >>> 23);
        charRes = (@Signed char) (signed >> 23);

        // Shifting right by 24, the introduced bits are still cast away.
        charRes = (@Unsigned char) (unsigned >>> 24);
        charRes = (@Unsigned char) (unsigned >> 24);
        charRes = (@Signed char) (signed >>> 24);
        charRes = (@Signed char) (signed >> 24);

        // Shifting right by 25, now the MSB matters.
        charRes = (@Unsigned char) (unsigned >>> 25);

        //:: error: (shift.signed)
        charRes = (@Unsigned char) (unsigned >> 25);

        //:: error: (shift.unsigned)
        charRes = (@Signed char) (signed >>> 25);
        charRes = (@Signed char) (signed >> 25);

        // Shifting right by zero should behave as assignment
        charRes = (@Unsigned char) (unsigned >>> 0);
        charRes = (@Unsigned char) (unsigned >> 0);
        charRes = (@Signed char) (signed >>> 0);
        charRes = (@Signed char) (signed >> 0);

        // Cast to short.
        @UnknownSignedness short shortRes;

        // Shifting right by 15, the introduced bits are cast away
        shortRes = (@Unsigned short) (unsigned >>> 15);
        shortRes = (@Unsigned short) (unsigned >> 15);
        shortRes = (@Signed short) (signed >>> 15);
        shortRes = (@Signed short) (signed >> 15);

        // Shifting right by 16, the introduced bits are still cast away.
        shortRes = (@Unsigned short) (unsigned >>> 16);
        shortRes = (@Unsigned short) (unsigned >> 16);
        shortRes = (@Signed short) (signed >>> 16);
        shortRes = (@Signed short) (signed >> 16);

        // Shifting right by 17, now the MSB matters.
        shortRes = (@Unsigned short) (unsigned >>> 17);

        //:: error: (shift.signed)
        shortRes = (@Unsigned short) (unsigned >> 17);

        //:: error: (shift.unsigned)
        shortRes = (@Signed short) (signed >>> 17);
        shortRes = (@Signed short) (signed >> 17);

        // Shifting right by zero should behave as assignment
        shortRes = (@Unsigned short) (unsigned >>> 0);
        shortRes = (@Unsigned short) (unsigned >> 0);
        shortRes = (@Signed short) (signed >>> 0);
        shortRes = (@Signed short) (signed >> 0);

        // Cast to int.
        @UnknownSignedness int intRes;

        // Now shift signedness matters again
        intRes = (@Unsigned int) (unsigned >>> 1);

        //:: error: (shift.signed)
        intRes = (@Unsigned int) (unsigned >> 1);

        //:: error: (shift.unsigned)
        intRes = (@Signed int) (signed >>> 1);
        intRes = (@Signed int) (signed >> 1);

        // Shifting right by zero should behave as assignment
        intRes = (@Unsigned int) (unsigned >>> 0);
        intRes = (@Unsigned int) (unsigned >> 0);
        intRes = (@Signed int) (signed >>> 0);
        intRes = (@Signed int) (signed >> 0);

        // Cast to long.
        @UnknownSignedness long longRes;

        // Now shift signedness matters again
        longRes = (@Unsigned long) (unsigned >>> 1);

        //:: error: (shift.signed)
        longRes = (@Unsigned long) (unsigned >> 1);

        //:: error: (shift.unsigned)
        longRes = (@Signed long) (signed >>> 1);
        longRes = (@Signed long) (signed >> 1);

        // Shifting right by zero should behave as assignment
        longRes = (@Unsigned long) (unsigned >>> 0);
        longRes = (@Unsigned long) (unsigned >> 0);
        longRes = (@Signed long) (signed >>> 0);
        longRes = (@Signed long) (signed >> 0);

        // Tests with double parenthesis (only byte and int)

        // Cast to byte.
        // Shifting right by 23, the introduced bits are cast away
        byteRes = (@Unsigned byte) ((unsigned >>> 23));
        byteRes = (@Unsigned byte) ((unsigned >> 23));
        byteRes = (@Signed byte) ((signed >>> 23));
        byteRes = (@Signed byte) ((signed >> 23));

        // Shifting right by 24, the introduced bits are still cast away.
        byteRes = (@Unsigned byte) ((unsigned >>> 24));
        byteRes = (@Unsigned byte) ((unsigned >> 24));
        byteRes = (@Signed byte) ((signed >>> 24));
        byteRes = (@Signed byte) ((signed >> 24));

        // Shifting right by 25, now the MSB matters.
        byteRes = (@Unsigned byte) ((unsigned >>> 25));

        //:: error: (shift.signed)
        byteRes = (@Unsigned byte) ((unsigned >> 25));

        //:: error: (shift.unsigned)
        byteRes = (@Signed byte) ((signed >>> 25));
        byteRes = (@Signed byte) ((signed >> 25));

        // Shifting right by zero should behave as assignment
        byteRes = (@Unsigned byte) ((unsigned >>> 0));
        byteRes = (@Unsigned byte) ((unsigned >> 0));
        byteRes = (@Signed byte) ((signed >>> 0));
        byteRes = (@Signed byte) ((signed >> 0));

        // Cast to int.
        // Now shift signedness matters again
        intRes = (@Unsigned int) ((unsigned >>> 1));

        //:: error: (shift.signed)
        intRes = (@Unsigned int) ((unsigned >> 1));

        //:: error: (shift.unsigned)
        intRes = (@Signed int) ((signed >>> 1));
        intRes = (@Signed int) ((signed >> 1));

        // Shifting right by zero should behave as assignment
        intRes = (@Unsigned int) ((unsigned >>> 0));
        intRes = (@Unsigned int) ((unsigned >> 0));
        intRes = (@Signed int) ((signed >>> 0));
        intRes = (@Signed int) ((signed >> 0));

        // Test outside Java Specification shift ranges
        // Cast to int.
        // Now shift signedness matters again
        intRes = (@Unsigned int) ((unsigned >>> 33));

        //:: error: (shift.signed)
        intRes = (@Unsigned int) ((unsigned >> 33));

        //:: error: (shift.unsigned)
        intRes = (@Signed int) ((signed >>> 33));
        intRes = (@Signed int) ((signed >> 33));

        // Shifting right by zero should behave as assignment
        intRes = (@Unsigned int) ((unsigned >>> 32));
        intRes = (@Unsigned int) ((unsigned >> 32));
        intRes = (@Signed int) ((signed >>> 32));
        intRes = (@Signed int) ((signed >> 32));
    }

    public void CastedLongShifts(@Unsigned long unsigned, @Signed long signed) {
        // Cast to byte.
        @UnknownSignedness byte byteRes;

        // Shifting right by 55, the introduced bits are cast away
        byteRes = (@Unsigned byte) (unsigned >>> 55);
        byteRes = (@Unsigned byte) (unsigned >> 55);
        byteRes = (@Signed byte) (signed >>> 55);
        byteRes = (@Signed byte) (signed >> 55);

        // Shifting right by 56, the introduced bits are still cast away.
        byteRes = (@Unsigned byte) (unsigned >>> 56);
        byteRes = (@Unsigned byte) (unsigned >> 56);
        byteRes = (@Signed byte) (signed >>> 56);
        byteRes = (@Signed byte) (signed >> 56);

        // Shifting right by 57, now the MSB matters.
        byteRes = (@Unsigned byte) (unsigned >>> 57);

        //:: error: (shift.signed)
        byteRes = (@Unsigned byte) (unsigned >> 57);

        //:: error: (shift.unsigned)
        byteRes = (@Signed byte) (signed >>> 57);
        byteRes = (@Signed byte) (signed >> 57);

        // Shifting right by zero should behave as assignment
        byteRes = (@Unsigned byte) (unsigned >>> 0);
        byteRes = (@Unsigned byte) (unsigned >> 0);
        byteRes = (@Signed byte) (signed >>> 0);
        byteRes = (@Signed byte) (signed >> 0);

        // Cast to char.
        @UnknownSignedness char charRes;

        // Shifting right by 55, the introduced bits are cast away
        charRes = (@Unsigned char) (unsigned >>> 55);
        charRes = (@Unsigned char) (unsigned >> 55);
        charRes = (@Signed char) (signed >>> 55);
        charRes = (@Signed char) (signed >> 55);

        // Shifting right by 56, the introduced bits are still cast away.
        charRes = (@Unsigned char) (unsigned >>> 56);
        charRes = (@Unsigned char) (unsigned >> 56);
        charRes = (@Signed char) (signed >>> 56);
        charRes = (@Signed char) (signed >> 56);

        // Shifting right by 57, now the MSB matters.
        charRes = (@Unsigned char) (unsigned >>> 57);

        //:: error: (shift.signed)
        charRes = (@Unsigned char) (unsigned >> 57);

        //:: error: (shift.unsigned)
        charRes = (@Signed char) (signed >>> 57);
        charRes = (@Signed char) (signed >> 57);

        // Shifting right by zero should behave as assignment
        charRes = (@Unsigned char) (unsigned >>> 0);
        charRes = (@Unsigned char) (unsigned >> 0);
        charRes = (@Signed char) (signed >>> 0);
        charRes = (@Signed char) (signed >> 0);

        // Cast to short.
        @UnknownSignedness short shortRes;

        // Shifting right by 47, the introduced bits are cast away
        shortRes = (@Unsigned short) (unsigned >>> 47);
        shortRes = (@Unsigned short) (unsigned >> 47);
        shortRes = (@Signed short) (signed >>> 47);
        shortRes = (@Signed short) (signed >> 47);

        // Shifting right by 48, the introduced bits are still cast away.
        shortRes = (@Unsigned short) (unsigned >>> 48);
        shortRes = (@Unsigned short) (unsigned >> 48);
        shortRes = (@Signed short) (signed >>> 48);
        shortRes = (@Signed short) (signed >> 48);

        // Shifting right by 49, now the MSB matters.
        shortRes = (@Unsigned short) (unsigned >>> 49);

        //:: error: (shift.signed)
        shortRes = (@Unsigned short) (unsigned >> 49);

        //:: error: (shift.unsigned)
        shortRes = (@Signed short) (signed >>> 49);
        shortRes = (@Signed short) (signed >> 49);

        // Shifting right by zero should behave as assignment
        shortRes = (@Unsigned short) (unsigned >>> 0);
        shortRes = (@Unsigned short) (unsigned >> 0);
        shortRes = (@Signed short) (signed >>> 0);
        shortRes = (@Signed short) (signed >> 0);

        // Cast to int.
        @UnknownSignedness int intRes;

        // Shifting right by 31, the introduced bits are cast away
        intRes = (@Unsigned int) (unsigned >>> 31);
        intRes = (@Unsigned int) (unsigned >> 31);
        intRes = (@Signed int) (signed >>> 31);
        intRes = (@Signed int) (signed >> 31);

        // Shifting right by 32, the introduced bits are still cast away.
        intRes = (@Unsigned int) (unsigned >>> 32);
        intRes = (@Unsigned int) (unsigned >> 32);
        intRes = (@Signed int) (signed >>> 32);
        intRes = (@Signed int) (signed >> 32);

        // Shifting right by 33, now the MSB matters.
        intRes = (@Unsigned int) (unsigned >>> 33);

        //:: error: (shift.signed)
        intRes = (@Unsigned int) (unsigned >> 33);

        //:: error: (shift.unsigned)
        intRes = (@Signed int) (signed >>> 33);
        intRes = (@Signed int) (signed >> 33);

        // Shifting right by zero should behave as assignment
        intRes = (@Unsigned int) (unsigned >>> 0);
        intRes = (@Unsigned int) (unsigned >> 0);
        intRes = (@Signed int) (signed >>> 0);
        intRes = (@Signed int) (signed >> 0);

        // Cast to long.
        @UnknownSignedness long longRes;

        // Now shift signedness matters again
        longRes = (@Unsigned long) (unsigned >>> 1);

        //:: error: (shift.signed)
        longRes = (@Unsigned long) (unsigned >> 1);

        //:: error: (shift.unsigned)
        longRes = (@Signed long) (signed >>> 1);
        longRes = (@Signed long) (signed >> 1);

        // Shifting right by zero should behave as assignment
        longRes = (@Unsigned long) (unsigned >>> 0);
        longRes = (@Unsigned long) (unsigned >> 0);
        longRes = (@Signed long) (signed >>> 0);
        longRes = (@Signed long) (signed >> 0);

        // Tests with double parenthesis (only byte and long)

        // Cast to byte.
        // Shifting right by 55, the introduced bits are cast away
        byteRes = (@Unsigned byte) ((unsigned >>> 55));
        byteRes = (@Unsigned byte) ((unsigned >> 55));
        byteRes = (@Signed byte) ((signed >>> 55));
        byteRes = (@Signed byte) ((signed >> 55));

        // Shifting right by 56, the introduced bits are still cast away.
        byteRes = (@Unsigned byte) ((unsigned >>> 56));
        byteRes = (@Unsigned byte) ((unsigned >> 56));
        byteRes = (@Signed byte) ((signed >>> 56));
        byteRes = (@Signed byte) ((signed >> 56));

        // Shifting right by 9, now the MSB matters.
        byteRes = (@Unsigned byte) ((unsigned >>> 57));

        //:: error: (shift.signed)
        byteRes = (@Unsigned byte) ((unsigned >> 57));

        //:: error: (shift.unsigned)
        byteRes = (@Signed byte) ((signed >>> 57));
        byteRes = (@Signed byte) ((signed >> 57));

        // Shifting right by zero should behave as assignment
        byteRes = (@Unsigned byte) ((unsigned >>> 0));
        byteRes = (@Unsigned byte) ((unsigned >> 0));
        byteRes = (@Signed byte) ((signed >>> 0));
        byteRes = (@Signed byte) ((signed >> 0));

        // Cast to long.
        // Now shift signedness matters again
        longRes = (@Unsigned long) ((unsigned >>> 1));

        //:: error: (shift.signed)
        longRes = (@Unsigned long) ((unsigned >> 1));

        //:: error: (shift.unsigned)
        longRes = (@Signed long) ((signed >>> 1));
        longRes = (@Signed long) ((signed >> 1));

        // Shifting right by zero should behave as assignment
        longRes = (@Unsigned long) ((unsigned >>> 0));
        longRes = (@Unsigned long) ((unsigned >> 0));
        longRes = (@Signed long) ((signed >>> 0));
        longRes = (@Signed long) ((signed >> 0));

        // Test outside Java Specification shift ranges
        // Cast to long.
        // Now shift signedness matters again
        longRes = (@Unsigned long) ((unsigned >>> 65));

        //:: error: (shift.signed)
        longRes = (@Unsigned long) ((unsigned >> 65));

        //:: error: (shift.unsigned)
        longRes = (@Signed long) ((signed >>> 65));
        longRes = (@Signed long) ((signed >> 65));

        // Shifting right by zero should behave as assignment
        longRes = (@Unsigned long) ((unsigned >>> 64));
        longRes = (@Unsigned long) ((unsigned >> 64));
        longRes = (@Signed long) ((signed >>> 64));
        longRes = (@Signed long) ((signed >> 64));
    }
}
