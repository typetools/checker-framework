import org.checkerframework.common.value.qual.*;

class TransferMod {

    void test() {
        int aa = -100;
        int a = -1;
        int b = 0;
        int c = 1;
        int d = 2;

        @IntRange(from = 1) int e = 5 % 3;
        @IntRange(from = 0) int f = -100 % 1;

        @IntRange(from = 0) int g = aa % -1;
        @IntRange(from = 0) int h = aa % 1;
        @IntRange(from = 0) int i = d % -1;
        @IntRange(from = 0) int j = d % 1;

        @IntRange(from = 0) int k = d % c;
        @IntRange(from = 0) int l = b % c;
        @IntRange(from = 0) int m = c % d;

        @IntRange(from = 0) int n = c % a;
        @IntRange(from = 0) int o = b % a;

        @IntRange(from = -1) int p = a % a;
        @IntRange(from = -1) int q = a % d;
        @IntRange(from = -1) int r = a % c;
    }
}
