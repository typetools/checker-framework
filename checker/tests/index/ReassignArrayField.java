import org.checkerframework.checker.index.qual.*;

class ReassignArrayField {
    class PrF {
        private final int[] a;

        public PrF() {
            a = new int[0];
        }

        void test0(PrF p) {}

        void test1(PrF p, @IndexFor("#1.a") int i) {
            int k = p.a[i];
        }

        void test2(PrF p, int i) {
            for (i = 0; i < p.a.length; i++) {
                int k = p.a[i];
            }
        }
    }

    class PuF {
        public final int[] a;

        public PuF() {
            a = new int[0];
        }
    }

    class Pr {
        private int[] a, b, c;

        public Pr() {
            a = new int[0];
        }

        void test0(Pr p) {}

        void test1(Pr p, @IndexFor("#1.a") int i) {
            int k = p.a[i];
        }

        void test2(Pr p, int i) {
            for (i = 0; i < p.a.length; i++) {
                int k = p.a[i];
            }
        }

        void test02(Pr p) {
            p.b = new int[0];
        }

        void test12(Pr p, @IndexFor("#1.a") int i) {
            //:: error: (reassignment.not.permitted)
            p.a = new int[0];
            int k = p.a[i];
            p.b = new int[0];
        }

        void test22(Pr p, int i) {
            for (i = 0; i < p.c.length; i++) {
                p.c = new int[0];
                //:: error: (array.access.unsafe.high.range)
                int k = p.c[i];
            }
            p.b = new int[0];
        }
    }

    class Pu {
        public int[] a, b;

        public Pu() {
            a = new int[0];
        }
    }

    void test0(PuF p) {}

    void test1(PuF p, @IndexFor("#1.a") int i) {
        int k = p.a[i];
    }

    void test2(PuF p, int i) {
        for (i = 0; i < p.a.length; i++) {
            int k = p.a[i];
        }
    }

    void test01(Pu p) {}

    void test21(Pu p, int i) {
        for (i = 0; i < p.a.length; i++) {
            int k = p.a[i];
        }
    }

    void test02(Pu p) {
        p.b = new int[0];
    }

    void test22(Pu p, int i) {
        for (i = 0; i < p.a.length; i++) {
            p.a = new int[0];
            //:: error: (array.access.unsafe.high.range)
            int k = p.a[i];
        }
        p.b = new int[0];
    }
}
