import org.checkerframework.common.value.qual.*;

public class ArrayInit {
    public void raggedArrays() {
        int @ArrayLen(4) [] @ArrayLen({1, 3, 4}) [] @ArrayLen({1, 2, 3, 4, 7}) [] alpha =
                new int[][][] {
                    {{1, 1}, {1, 1, 1}, {1}, {1}},
                    {{1}, {1}, {1}, {1, 1}},
                    {{1, 2, 3, 4, 5, 6, 7}},
                    {{1}, {1}, {1, 1, 1, 1}}
                };

        int @ArrayLen(4) [] @ArrayLen({1, 3, 4}) [] @ArrayLenRange(from = 1, to = 12) [] gamma =
                new int[][][] {
                    {{1, 1}, {1, 1, 1}, {1}, {1}},
                    {{1}, {1}, {1}, {1, 1}},
                    {{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12}},
                    {{1}, {1}, {1, 1, 1, 1}}
                };

        int @ArrayLen(4) [] @ArrayLen({1, 2, 3}) [] a = {{1, 1}, {1, 1, 1}, {1}, {1}};
        int @ArrayLen(4) [] @ArrayLen({1, 2}) [] b = {{1}, {1}, {1}, {1, 1}};
        int @ArrayLen(1) [] @ArrayLen(7) [] c = {{1, 2, 3, 4, 5, 6, 7}};
        int @ArrayLen(3) [] @ArrayLen({1, 4}) [] d = {{1}, {1}, {1, 1, 1, 1}};

        int @ArrayLen(4) [] @ArrayLen({1, 3, 4}) [] @ArrayLen({1, 2, 3, 4, 7}) [] beta = {
            a, b, c, d
        };

        int @ArrayLen(4) [] @ArrayLen({1, 2, 3}) [] a1 = {{1, 1}, {1, 1, 1}, {1}, {1}};
        int @ArrayLen(4) [] @ArrayLen({1, 2}) [] b1 = {{1}, {1}, {1}, {1, 1}};
        int @ArrayLen(1) [] @ArrayLen(11) [] c1 = {{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11}};
        int @ArrayLen(3) [] @ArrayLen({1, 4}) [] d1 = {{1}, {1}, {1, 1, 1, 1}};

        int @ArrayLen(4) [] @ArrayLenRange(from = 1, to = 4) [] @ArrayLenRange(from = 1, to = 11) []
                delta = {a1, b1, c1, d1};
    }

    public void numberInit() {
        int @ArrayLen({1}) [] a = new int[1];
    }

    public void listInit() {
        int @ArrayLen({1}) [] a = new int[] {4};
    }

    public void varInit() {
        int i = 1;
        int @ArrayLen({1}) [] a = new int[i];
    }

    public void rangeInit(
            @IntRange(from = 1, to = 2) int shortLength,
            @IntRange(from = 1, to = 20) int longLength,
            @BottomVal int bottom) {
        int @ArrayLen({1, 2}) [] a = new int[shortLength];
        // :: error: (assignment.type.incompatible)
        int @ArrayLen({1, 2}) [] b = new int[longLength];
        int @ArrayLenRange(from = 1, to = 20) [] d = new int[longLength];
        int @ArrayLen({0}) [] c = new int[bottom];
    }

    public void multiDim() {
        int i = 2;
        int j = 3;
        int @ArrayLen({2}) [] @ArrayLen({3}) [] a = new int[2][3];
        int @ArrayLen({2}) [] @ArrayLen({3}) [] b = new int[i][j];

        int @ArrayLen({2}) [][] c = new int[][] {{2}, {3}};
    }

    public void initilizer() {
        int @ArrayLen(3) [] ints = new int[] {2, 2, 2};
        char @StringVal("-A%") [] chars = new char[] {45, 'A', '%'};
        int @ArrayLen(3) [] ints2 = {2, 2, 2};
    }

    public void initializerString() {
        byte @ArrayLen(2) [] bytes = new byte[] {100, '%'};
        char @ArrayLen(3) [] chars = new char[] {45, 'A', '%'};
    }

    public void vargsTest() {
        // type of arg should be @UnknownVal Object @BottomVal[]
        vargs((Object[]) null);

        // type of arg should be @UnknownVal int @BottomVal[]
        vargs((int[]) null);

        // type of arg should be @UnknownVal byte @BottomVal[]
        vargs((byte[]) null);
    }

    public void vargs(Object @ArrayLen(0) ... objs) {}

    public void vargs(int @ArrayLen(0) ... ints) {}

    public void vargs(byte @ArrayLen(0) ... bytes) {}

    public void nullableArrays() {
        Object @ArrayLen(2) [] @ArrayLen(1) [] o = new Object[][] {new Object[] {null}, null};
        Object @ArrayLen(1) [][] o2 = new Object[][] {null};
        Object @ArrayLen(1) [] @ArrayLen(1) [] o3 = new Object[][] {null};
    }

    public void subtyping1(int @ArrayLen({1, 5}) [] a) {
        int @ArrayLenRange(from = 1, to = 5) [] b = a;
        // :: error: (assignment.type.incompatible)
        int @ArrayLenRange(from = 2, to = 5) [] c = a;
    }

    public void subtyping2(int @ArrayLenRange(from = 1, to = 5) [] a) {
        int @ArrayLen({1, 2, 3, 4, 5}) [] b = a;
        // :: error: (assignment.type.incompatible)
        int @ArrayLen({1, 5}) [] c = a;
    }

    public void subtyping3(int @ArrayLenRange(from = 1, to = 17) [] a) {
        // :: error: (assignment.type.incompatible)
        int @ArrayLenRange(from = 1, to = 12) [] b = a;
        // :: error: (assignment.type.incompatible)
        int @ArrayLenRange(from = 5, to = 18) [] c = a;
        int @ArrayLenRange(from = 0, to = 20) [] d = a;
    }

    public void lub1(boolean flag, @IntRange(from = 1, to = 200) int x) {
        int[] a;
        if (flag) {
            a = new int[x];
        } else {
            a = new int[250];
        }

        int @ArrayLenRange(from = 1, to = 250) [] b = a;
    }

    public void lub2(
            boolean flag, @IntRange(from = 1, to = 20) int x, @IntRange(from = 50, to = 75) int y) {
        int[] a;
        if (flag) {
            a = new int[x];
        } else {
            a = new int[y];
        }

        int @ArrayLenRange(from = 1, to = 75) [] b = a;
    }

    public void lub3(
            boolean flag, @IntRange(from = 1, to = 5) int x, @IntRange(from = 3, to = 7) int y) {
        int[] a;
        if (flag) {
            a = new int[x];
        } else {
            a = new int[y];
        }

        int @ArrayLenRange(from = 1, to = 7) [] b = a;
        int @ArrayLen({1, 2, 3, 4, 5, 6, 7}) [] c = a;
    }

    public void refine(int[] q) {
        if (q.length < 20) {
            @IntRange(from = 0, to = 19) int x = q.length;
            int @ArrayLenRange(from = 0, to = 19) [] b = q;
            if (q.length < 5) {
                int @ArrayLen({0, 1, 2, 3, 4}) [] c = q;
            }
        }
    }

    // The argument is an arraylen with too many values.
    // :: warning: (too.many.values.given)
    public void coerce(int @ArrayLen({1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 36}) [] a) {
        int @ArrayLenRange(from = 1, to = 36) [] b = a;
        if (a.length < 15) {
            // :: error: (assignment.type.incompatible)
            int @ArrayLen({1, 2, 3, 4, 5, 6, 7, 8, 9, 10}) [] c = a;
        }
    }

    public void warnings() {
        // :: warning: (negative.arraylen)
        int @ArrayLenRange(from = -1, to = 5) [] a;
        // :: error: (from.greater.than.to)
        int @ArrayLenRange(from = 10, to = 3) [] b;
    }
}
