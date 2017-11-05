import org.checkerframework.common.value.qual.IntRange;

public class Issue1623 {
    public static void hexEncode(byte[] bytes) {
        for (int i = 0; i < bytes.length; i++) {
            byte b = bytes[i];
            @IntRange(from = 0, to = 15) int i1 = (b & 0xf0) >> 4;
            @IntRange(from = 0, to = 15) int i2 = b & 0x0f;
        }
    }
}
