import org.checkerframework.common.value.qual.IntVal;
import org.checkerframework.common.value.qual.StringVal;

public class ByteArrayWithNonLiteralConstants {

    public static void main(String[] args) {
        byte @StringVal("hello") [] greeting1 = {'h', 'e', 'l', 'l', 'o'};
        @IntVal('e') byte e = 'e';
        byte @StringVal("hello") [] greeting2 = {'h', e, 'l', 'l', 'o'};
    }
}
