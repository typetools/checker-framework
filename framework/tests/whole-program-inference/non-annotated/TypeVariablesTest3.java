import testlib.wholeprograminference.qual.*;
import testlib.wholeprograminference.qual.Sibling2;

class Generic<@Sibling1 T extends @Sibling1 Object> {
    public @Sibling2 T sibling2;
    public @Sibling1 T sibling1;

    public T tField;

    void foo(T param) {
        // :: error: (assignment.type.incompatible)
        param = sibling2;
    }

    void baz(T param) {
        param = sibling1;
    }

    void bar(@Sibling2 T param) {
        // :: error: (assignment.type.incompatible)
        tField = param;
    }
}
