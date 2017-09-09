import org.checkerframework.common.value.qual.*;

public class TransferTimes {

    void test() {
        int a = 1;
        @IntRange(from = 1) int b = a * 1;
        @IntRange(from = 1) int c = 1 * a;
        @IntRange(from = 0) int d = 0 * a;
        //:: error: (assignment.type.incompatible)
        @IntRange(from = 0) int e = -1 * a;

        int g = -1;
        @IntRange(from = 0) int h = g * 0;
        //:: error: (assignment.type.incompatible)
        @IntRange(from = 1) int i = g * 0;
        //:: error: (assignment.type.incompatible)
        @IntRange(from = 1) int j = g * a;

        int k = 0;
        int l = 1;
        @IntRange(from = 1) int m = a * l;
        @IntRange(from = 0) int n = k * l;
        @IntRange(from = 0) int o = k * k;
    }
}
