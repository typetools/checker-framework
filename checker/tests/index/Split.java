import java.util.regex.Pattern;
import org.checkerframework.common.value.qual.MinLen;

public class Split {
    Pattern p = Pattern.compile(".*");

    void test() {
        String @MinLen(1) [] s = p.split("sdf");
    }
}
