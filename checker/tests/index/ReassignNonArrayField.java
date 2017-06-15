import org.checkerframework.checker.index.qual.*;
import org.checkerframework.dataflow.qual.Pure;

class ReassignNonArrayField {
    class P {
        public Q q;
    }

    class P2 extends P {
        //:: warning: (dependent.not.permitted)
        void m(@IndexFor("id(this.q.a)") int x) {}

        @Pure
        int[] id(int[] a) {
            return a;
        }
    }

    class Q {
        public int[] a;
    }

    @Pure
    int[] id(int[] a) {
        return a;
    }

    void test(int[] b, @IndexFor("#1") int x, P p) {
        if (x < this.id(b).length) {
            p.q = new Q();
            //:: error: (array.access.unsafe.high)
            int j = this.id(b)[x];
        }
    }

    void test1(int[] b, @IndexFor("id(#1)") int x, P p) {
        int i = this.id(b)[x];
        // This reassignment is permitted, because the IndexFor above is
        // not in the enclosing class of p.q (which is P).
        p.q = new Q();
        int j = this.id(b)[x];
    }

    void test12(int[] b, @IndexFor("id(#1)") int x, P2 p) {
        int i = this.id(b)[x];
        // No warning issued here despite this being unsafe. A warning is
        // issued above when P2 is defined.
        p.q = new Q();
        int j = this.id(b)[x];
    }

    //:: warning: (dependent.not.permitted)
    void test2(@IndexFor("#2.q.a") int x, P p1, P p2) {
        int i = p1.q.a[x];
        p2.q = new Q();
        // Note that this is unsafe, but the warning above is issued instead.
        int j = p1.q.a[x];
    }
}
