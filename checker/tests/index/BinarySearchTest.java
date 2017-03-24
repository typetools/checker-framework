import java.util.Arrays;
import org.checkerframework.checker.index.qual.*;

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

    public void test(short[] a, short instant) {
        int i = Arrays.binarySearch(a, instant);
        @SearchIndex("a")
        int z = i;
        //:: error: (assignment.type.incompatible)
        @SearchIndex("a")
        int y = 7;
        @LTLengthOf("a") int x = i;
    }

    void test2(int[] a, @SearchIndex("#1") int xyz) {
        if (0 > xyz) {
            @NegativeIndexFor("a")
            int w = xyz;
            @NonNegative int y = ~xyz;
            @LTEqLengthOf("a") int z = ~xyz;
        }
    }
}
