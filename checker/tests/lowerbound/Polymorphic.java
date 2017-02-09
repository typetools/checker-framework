import org.checkerframework.checker.index.qual.*;

class Polymorphic {

    //Identity functions

    @PolyLowerBound
    int lbc_identity(@PolyLowerBound int a) {
        return a;
    }

    // LowerBound tests
    void lbc_id(@NonNegative int n, @Positive int p, @GTENegativeOne int g) {
        @NonNegative int an = lbc_identity(n);
        //:: error: (assignment.type.incompatible)
        @Positive int bn = lbc_identity(n);

        @GTENegativeOne int ag = lbc_identity(g);
        //:: error: (assignment.type.incompatible)
        @NonNegative int bg = lbc_identity(g);

        @Positive int ap = lbc_identity(p);
    }
}
