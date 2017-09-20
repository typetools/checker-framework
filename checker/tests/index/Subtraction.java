import org.checkerframework.checker.index.qual.LTLengthOf;
import org.checkerframework.common.value.qual.MinLen;

// @skip-test until the type system is enriched so it can express either
//   * N = Grid.length and N-1 = Grid.length-1, or
//   * i < N  and i <= N-1

public class Subtraction {

    // Version without annotations
    public static void main(String[] args) {
        int N = 8;
        int[] grid = new int[N];
        for (int i = 0; i < N; i++) {
            System.out.println(grid[(N - 1) - i]);
        }
    }

    // Version with annotations
    public static void mainAnnotated(String[] args) {
        int N = 8;
        int @MinLen(8) [] grid = new int[N];
        @SuppressWarnings("upperbound")
        @LTLengthOf("grid") int zero = 0;
        for (@LTLengthOf("grid") int i = zero; i < N; i++) {
            System.out.println(grid[(N - 1) - i]);
            System.out.println(grid[(N - i)]);
            System.out.println(grid[(N - i) - 1]);
        }
    }
}
