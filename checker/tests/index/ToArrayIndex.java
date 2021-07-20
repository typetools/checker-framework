import org.checkerframework.common.value.qual.MinLen;

import java.util.ArrayList;

// @skip-test until we bring list support back

public class ToArrayIndex {

    public String @MinLen(1) [] m(@MinLen(1) ArrayList<String> compiler) {
        return compiler.toArray(new String[0]);
    }
}
