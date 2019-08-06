import org.checkerframework.common.value.qual.IntVal;
import org.checkerframework.common.value.qual.StringVal;

public class ByteArrayWithNonLiteralConstants {

    public static void main(String[] args) {
        char @StringVal("hello") [] greeting1 = {'h', 'e', 'l', 'l', 'o'};
        @IntVal('e') byte e = 'e';
        char @StringVal("hello") [] greeting2 = {'h', e, 'l', 'l', 'o'};
    }
}
