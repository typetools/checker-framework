import org.checkerframework.checker.minlen.qual.*;
import org.checkerframework.checker.upperbound.qual.*;

// @skip-test until the bug is fixed

public class ArrayLength2 {
    public static void main(String[] args) {
        int N = 8;
        int @MinLen(8) [] Grid = new int[N];
        @LessThanLength("Grid") int i = 0;
    }
}
