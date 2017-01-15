import org.checkerframework.checker.lowerbound.qual.*;

public class NegativeArray {

    public static void negativeArray(@GTENegativeOne int len) {
        //:: error: (array.length.negative)
        int[] arr = new int[len];
    }
}
