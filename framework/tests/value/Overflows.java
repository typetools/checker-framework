import org.checkerframework.common.value.qual.*;

public class Overflows {

    static void bytes() {
        byte max = Byte.MAX_VALUE;
        // :: warning: (cast.unsafe)
        @IntVal(-128) byte maxPlus1 = (byte) (max + 1);
    }

    static void chars() {
        char max = Character.MAX_VALUE;
        // :: warning: (cast.unsafe)
        @IntVal(0) char maxPlus1 = (char) (max + 1);
    }

    static void shorts() {
        short max = Short.MAX_VALUE;
        // :: warning: (cast.unsafe)
        @IntVal(-32768) short maxPlus1 = (short) (max + 1);
    }

    static void ints() {
        int max = Integer.MAX_VALUE;
        @IntVal(-2147483648) int maxPlus1 = max + 1;
    }

    static void longs() {
        long max = Long.MAX_VALUE;
        @IntVal(-9223372036854775808L) long maxPlus1 = max + 1;
    }

    static void doubles() {
        double max = Double.MAX_VALUE;
        @DoubleVal(1.7976931348623157E308) double maxPlus1 = max + 1.0;
    }

    static void floats() {
        float max = Float.MAX_VALUE;
        @DoubleVal(3.4028235E38f) float maxPlus1 = max + 1.0f;
    }
}
