import org.checkerframework.checker.nullness.qual.*;

import java.util.Collection;

public class ArrayNew {
    void m(Collection<? extends @NonNull Integer> seq1) {
        Integer[] seq1_array = new @NonNull Integer[] {5};
        Integer[] seq2_array = seq1.toArray(new @NonNull Integer[0]);
    }

    void takePrim1d(int[] ar) {}

    void callPrim1d() {
        takePrim1d(new int[] {1, 2, 1});
    }

    void takePrim2d(int[][] ar) {}

    void callPrim2d() {
        takePrim2d(new int[][] {{1, 2, 1}, {3, 3, 7}});
    }
}
