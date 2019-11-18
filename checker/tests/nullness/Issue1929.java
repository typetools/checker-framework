// Test case for Issue 1929:
// https://github.com/typetools/checker-framework/issues/1929

import java.util.Collection;
import org.checkerframework.common.value.qual.ArrayLen;

public class Issue1929 {

    String[] works1(Collection<String> c) {
        return c.toArray(new String[0]);
    }

    private static final String @ArrayLen(0) [] EMPTY_STRING_ARRAY_2 = new String[0];

    String[] fails2(Collection<String> c) {
        return c.toArray(EMPTY_STRING_ARRAY_2);
    }

    private static final String[] EMPTY_STRING_ARRAY_3 = new String[0];

    String[] fails3(Collection<String> c) {
        // We don't determine field types from initializition expressions.
        // :: error: (return.type.incompatible)
        return c.toArray(EMPTY_STRING_ARRAY_3);
    }
}
