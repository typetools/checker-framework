public class ShiftAndMask {

    void m(long longValue) {
        byte b1 = (byte) ((longValue >>> 32) & 0xFF);
        byte b2 = (byte) ((longValue >>> 40) & 0xFF);
    }
}
