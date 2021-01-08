import org.checkerframework.checker.index.qual.GTENegativeOne;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.index.qual.Positive;

public class TransferMod {

    void test() {
        int aa = -100;
        int a = -1;
        int b = 0;
        int c = 1;
        int d = 2;

        @Positive int e = 5 % 3;
        @NonNegative int f = -100 % 1;

        @NonNegative int g = aa % -1;
        @NonNegative int h = aa % 1;
        @NonNegative int i = d % -1;
        @NonNegative int j = d % 1;

        @NonNegative int k = d % c;
        @NonNegative int l = b % c;
        @NonNegative int m = c % d;

        @NonNegative int n = c % a;
        @NonNegative int o = b % a;

        @GTENegativeOne int p = a % a;
        @GTENegativeOne int q = a % d;
        @GTENegativeOne int r = a % c;
    }
}
// a comment
