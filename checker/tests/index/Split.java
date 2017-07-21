// @skip-test TODO: reinstate before merge

import java.util.regex.Pattern;
import org.checkerframework.common.value.qual.MinLen;

public class Split {
    Pattern p = Pattern.compile(".*");

    void test() {
        @MinLen(1) String[] s = p.split("sdf");
    }
}
