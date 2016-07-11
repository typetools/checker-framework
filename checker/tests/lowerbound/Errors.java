import org.checkerframework.checker.lowerbound.qual.*;

public class Errors{

    void test() {
        int[] arr = new int[5];

        // unsafe
        @GTENegativeOne int n1p = -1;
        @LowerBoundUnknown int u = -10;

        // safe
        @NonNegative int nn = 0;
        @Positive int p = 1;

        //:: warning: (array.access.unsafe.low)
        int a = arr[n1p];

        //:: warning: (array.access.unsafe.low)
        int b = arr[u];

        int c = arr[nn];
        int d = arr[p];
    }

}
