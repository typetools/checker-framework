import org.checkerframework.common.value.qual.*;

class Underflows {
    static void bytes() {
        byte min = Byte.MIN_VALUE;
        // :: warning: (cast.unsafe)
        @IntVal(127) byte maxPlus1 = (byte) (min - 1);
    }

    static void chars() {
        char min = Character.MIN_VALUE;
        // :: warning: (cast.unsafe)
        @IntVal(65535) char maxPlus1 = (char) (min - 1);
    }

    static void shorts() {
        short min = Short.MIN_VALUE;
        // :: warning: (cast.unsafe)
        @IntVal(32767) short maxPlus1 = (short) (min - 1);
    }

    static void ints() {
        int min = Integer.MIN_VALUE;
        @IntVal(2147483647) int maxPlus1 = min - 1;
    }

    static void longs() {
        long min = Long.MIN_VALUE;
        @IntVal(9223372036854775807L) long maxPlus1 = min - 1;
    }

    static void doubles() {
        double min = Double.MIN_VALUE;
        @DoubleVal(-1.0) double maxPlus1 = min - 1.0;
    }

    static void floats() {
        float min = Float.MIN_VALUE;
        @DoubleVal(-1.0F) float maxPlus1 = min - 1.0f;
    }
}
