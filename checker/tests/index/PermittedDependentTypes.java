import org.checkerframework.checker.index.qual.*;
import org.checkerframework.dataflow.qual.Pure;

class PermittedDependentTypes {
    public int[] x;
    private int[] y;
    public final int[] z;

    public PermittedDependentTypes other;

    private PermittedDependentTypes pother;

    private PermittedDependentTypes() {
        z = new int[5];
    }

    @Pure
    int[] id(int[] x) {
        return x;
    }

    // impure method for testing
    int[] not_id(int[] y) {
        y = new int[4];
        return y;
    }

    //:: warning: (dependent.not.permitted)
    void foo(@IndexFor("x") int w) {
        // Not permitted
    }

    void bar(@IndexFor("y") int w) {
        // Permitted
    }

    void baz(@IndexFor("z") int w) {
        // Permitted
    }

    //:: warning: (dependent.not.permitted)
    void qux(@IndexFor("id(x)") int w) {
        // Not permitted
    }

    //:: warning: (dependent.not.permitted)
    void xak(@IndexFor("other.id(y)") int w) {
        // Not permitted
    }

    void lop(@IndexFor("id(y)") int w) {
        // Permitted
    }

    //:: warning: (dependent.not.permitted)
    void zab(@IndexFor("not_id(y)") int w) {
        // Not permitted
    }

    //:: warning: (dependent.not.permitted)
    void zok(@IndexFor("other.z") int w) {
        // Not permitted
    }

    void fizz(@IndexFor("other.y") int w) {
        // Permitted
    }

    void buzz(@IndexFor("pother.z") int w) {
        // Permitted
    }

    void botch(@IndexFor("pother.y") int w) {
        // Permitted
    }

    void notch() {
        //:: warning: (local.variable.unsafe.dependent.annotation)
        @IndexFor("y") int w;
        @NonNegative int q;
    }
}
