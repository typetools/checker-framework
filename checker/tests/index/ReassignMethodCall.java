import org.checkerframework.checker.index.qual.*;
import org.checkerframework.dataflow.qual.Pure;

class ReassignMethodCall {
    class P {
        public Q q;
    }

    class Q {
        public int[] a;
    }

    @Pure
    int[] id(int[] a) {
        return a;
    }

    // it could do anything!
    void do_things() {}

    @Pure
    void do_nothing() {}

    void test(int[] b, @IndexFor("#1") int x, P p) {
        int i = b[x];
        if (x < this.id(b).length) {
            //:: error: (side.effect.invalidation)
            do_things();
            //:: error: (array.access.unsafe.high)
            int j = this.id(b)[x];
        }
    }

    void test1(int[] b, @IndexFor("id(#1)") int x, P p) {
        int i = this.id(b)[x];
        //:: error: (side.effect.invalidation)
        do_things();
        int j = this.id(b)[x];
    }

    void test2(int[] b, @NonNegative int i) {
        if (i < this.id(b).length) {
            do_nothing();
            int j = this.id(b)[i];
        }
    }

    void test3(int[] b, @IndexFor("id(#1)") int x, P p) {
        int i = this.id(b)[x];
        do_nothing();
        int j = this.id(b)[x];
    }
}
