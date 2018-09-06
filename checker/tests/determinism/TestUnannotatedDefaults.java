import org.checkerframework.checker.determinism.qual.*;

public class TestUnannotatedDefaults {
    static Object foo(@Det Object obj) {
        return obj;
    }

    static void bar(@Det Object obj) {
        @Det Object ret = foo(obj);
    }
}
