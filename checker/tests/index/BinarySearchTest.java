import org.checkerframework.checker.index.qual.*;

import java.util.Arrays;

public class BinarySearchTest {

    private final long @SameLen("iNameKeys") [] iTransitions;
    private final String @SameLen("iTransitions") [] iNameKeys;

    private BinarySearchTest(
            long @SameLen("iNameKeys") [] transitions,
            String @SameLen("iTransitions") [] nameKeys) {
        iTransitions = transitions;
        iNameKeys = nameKeys;
    }

    public String getNameKey(long instant) {
        long[] transitions = iTransitions;
        int i = Arrays.binarySearch(transitions, instant);
        if (i >= 0) {
            return iNameKeys[i];
        }
        i = ~i;
        if (i > 0) {
            return iNameKeys[i - 1];
        }
        return "";
    }

    public String getNameKey2(long instant) {
        long[] transitions = iTransitions;
        int i = Arrays.binarySearch(transitions, instant);
        if (i >= 0) {
            return iNameKeys[i];
        }
        i = ~i;
        if (i < iNameKeys.length) {
            return iNameKeys[i];
        }
        return "";
    }
}
