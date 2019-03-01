import java.util.List;
import org.checkerframework.checker.interning.qual.Interned;

/*
 * This test case excercises the interaction between class annotations
 * and method type argument inference.
 * A previously existing Unqualified annotation wasn't correctly removed.
 */
class ClassDefaults {
    // :: error: (super.invocation.invalid)
    @Interned class Test {}

    public static interface Visitor<T> {}

    class GuardingVisitor implements Visitor<List<Test>> {
        void call() {
            test(this);
        }
    }

    <T> T test(Visitor<T> p) {
        return null;
    }

    void call(GuardingVisitor p) {
        test(p);
    }
}
