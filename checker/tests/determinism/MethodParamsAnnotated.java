package determinism;

import org.checkerframework.checker.determinism.qual.*;

public class MethodParamsAnnotated {
    void callerMethod(@NonDet int a) {
        AnnotateMethod(a);
    }

    @Det
    int AnnotateMethod(@NonDet int a) {
        return 0;
    }
}
