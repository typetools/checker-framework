import java.util.Arrays;
import org.checkerframework.common.value.qual.MinLen;

public class PlumeFail {
    void method() {
        @SuppressWarnings({"index", "value"})
        String @MinLen(1) [] args = getArray();
        String[] argArray = Arrays.copyOfRange(args, 1, args.length);
    }

    String[] getArray() {
        return null;
    }
}
