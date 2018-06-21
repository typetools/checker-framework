import org.checkerframework.checker.determinism.qual.*;

public class TestEquals {
    void test1(@Det NodeD n, @Det NodeD m) {
        @Det boolean res = n.equals(m);
    }

    void test3(@Det NodeD n, @NonDet NodeD m) {
        // :: error: (assignment.type.incompatible)
        @Det boolean res = n.equals(m);
    }

    void test7(@NonDet NodeD n, @Det NodeD m) {
        // :: error: (assignment.type.incompatible)
        @Det boolean res = n.equals(m);
    }

    void test9(@NonDet NodeD n, @NonDet NodeD m) {
        // :: error: (assignment.type.incompatible)
        @Det boolean res = n.equals(m);
    }
}

class NodeD {
    int data;

    @Override
    public @PolyDet boolean equals(@PolyDet NodeD this, @PolyDet Object o) {
        return this.data == ((NodeD) o).data;
    }
}
