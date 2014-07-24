import org.checkerframework.checker.nullness.qual.*;
import org.checkerframework.framework.qual.*;

import java.util.List;

class MethodTypeVars4 {
    @DefaultQualifier(value=NonNull.class, locations=DefaultLocation.IMPLICIT_UPPER_BOUNDS)
    interface I {
        <T> T doit();
        <T> List<T> doit2();
        <T extends @Nullable Object> T doit3();
    }

    void f1(I i) {
        // s is implicitly Nullable
        String s = i.doit();
        List<String> ls = i.doit2();
        String s2 = i.doit3();
    }

    void f2(I i) {
        @NonNull String s = i.doit();
        s = i.doit3();
        //:: error: (type.argument.type.incompatible)
        List<@Nullable String> ls = i.doit2();
    }
}
