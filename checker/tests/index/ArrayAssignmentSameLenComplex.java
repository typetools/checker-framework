import org.checkerframework.checker.index.qual.IndexFor;
import org.checkerframework.checker.index.qual.NonNegative;

// @skip-test until #127 is resolved.

public class ArrayAssignmentSameLenComplex {

    static class Partial {
        private final int[] iValues;

        Partial(@NonNegative int n) {
            iValues = new int[n];
        }
    }

    private final Partial iBase;
    private final @IndexFor("iBase.iValues") int iFieldIndex;

    ArrayAssignmentSameLenComplex(Partial partial, @IndexFor("#1.iValues") int fieldIndex) {
        iBase = partial;
        iFieldIndex = fieldIndex;
    }
}
