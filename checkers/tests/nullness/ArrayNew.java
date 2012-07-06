import checkers.nullness.quals.*;
import java.util.Collection;

class ArrayNew {
    void m(Collection<? extends @NonNull Integer> seq1) {
        Integer[] seq1_array = new @NonNull Integer[10];
        Integer[] seq2_array = seq1.toArray(new @NonNull Integer[0]);
    }
}
