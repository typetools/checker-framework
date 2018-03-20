package determinism;

import org.checkerframework.checker.determinism.qual.*;

public class BinaryOp {
    void addDet(@Det int a, @Det int b) {
        System.out.println(a + b);
    }

    void addNonDet(@NonDet int a, @NonDet int b) {
        // :: error: (argument.type.incompatible)
        System.out.println(a + b);
    }

    void addDetNonDet(@Det int a, @NonDet int b) {
        // :: error: (argument.type.incompatible)
        System.out.println(a + b);
    }
}
