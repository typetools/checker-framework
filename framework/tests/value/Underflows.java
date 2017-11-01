import org.checkerframework.common.value.qual.*;

class Underflows {
    static void ints() {
        int max = Integer.MIN_VALUE;
        @IntVal(2147483647) int maxPlus1 = max - 1;
    }

    static void bytes() {
        byte max = Byte.MIN_VALUE;
        // :: warning: (cast.unsafe)
        @IntVal(127) byte maxPlus1 = (byte) (max - 1);
    }

    static void shorts() {
        short max = Short.MIN_VALUE;
        // :: warning: (cast.unsafe)
        @IntVal(32767) short maxPlus1 = (short) (max - 1);
    }

    static void longs() {
        long max = Long.MIN_VALUE;
        @IntVal(9223372036854775807L) long maxPlus1 = max - 1;
    }

    static void doubles() {
        double max = Double.MIN_VALUE;
        @DoubleVal(-1.0) double maxPlus1 = max - 1.0;
    }

    static void floats() {
        float max = Float.MIN_VALUE;
        @DoubleVal(-1.0F) float maxPlus1 = max - 1.0f;
    }
}
