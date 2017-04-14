import java.util.ArrayList;
import org.checkerframework.common.value.qual.*;

// @skip-test until we bring list support back

public class ToArray {

    public String @MinLen(1) [] m(@MinLen(1) ArrayList<String> compiler) {
        return compiler.toArray(new String[0]);
    }
}
