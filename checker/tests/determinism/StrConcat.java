package determinism;

import org.checkerframework.checker.determinism.qual.*;

public class StrConcat {
    void concat(@NonDet String a, @Det String b) {
        // :: error: (argument.type.incompatible)
        System.out.println(a + b);
    }
}
