import org.checkerframework.common.value.qual.MinLen;

import java.util.regex.Pattern;

public class Split {
    Pattern p = Pattern.compile(".*");

    void test() {
        String @MinLen(1) [] s = p.split("sdf");
    }
}
