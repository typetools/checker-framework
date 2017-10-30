import java.util.List;
import org.checkerframework.checker.nullness.qual.*;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.checkerframework.framework.qual.TypeUseLocation;

class MethodTypeVars4 {
    @DefaultQualifier(value = NonNull.class, locations = TypeUseLocation.IMPLICIT_UPPER_BOUND)
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
        // :: error: (type.argument.type.incompatible)
        List<@Nullable String> ls = i.doit2();
    }
}
