@SuppressWarnings("array.access.unsafe.high")
public class HexEncode {
    private static final char[] digits = {
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
    };

    public static String hexEncode(byte[] bytes) {
        StringBuffer s = new StringBuffer(bytes.length * 2);
        for (int i = 0; i < bytes.length; i++) {
            byte b = bytes[i];
            s.append(digits[(b & 0xf0) >> 4]);
            s.append(digits[b & 0x0f]);
        }
        return s.toString();
    }
}
