import org.checkerframework.checker.index.qual.GTENegativeOne;

public class NegativeArray {

    public static void negativeArray(@GTENegativeOne int len) {
        // :: error: (array.length.negative)
        int[] arr = new int[len];
    }
}
