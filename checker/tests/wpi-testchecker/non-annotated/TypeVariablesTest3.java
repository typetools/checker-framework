import org.checkerframework.checker.testchecker.wholeprograminference.qual.Sibling1;
import org.checkerframework.checker.testchecker.wholeprograminference.qual.Sibling2;

public class TypeVariablesTest3<@Sibling1 T extends @Sibling1 Object> {
    public @Sibling2 T sibling2;
    public @Sibling1 T sibling1;

    public T tField;

    void foo(T param) {
        // :: warning: (assignment.type.incompatible)
        param = sibling2;
    }

    void baz(T param) {
        param = sibling1;
    }

    void bar(@Sibling2 T param) {
        // :: warning: (assignment.type.incompatible)
        tField = param;
    }
}
