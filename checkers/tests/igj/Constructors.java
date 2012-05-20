import checkers.igj.quals.*;

public class Constructors {
    int field;

    // TODO: This test case doesn't test anything.
    // The constructor used to have an annotation, which is not legal
    // for a type annotation.
    // Also see ReadOnlyConstructor in javari.
    Constructors() {
        field = 0;
    }

    Constructors(@Immutable Constructors this, int a) {
        field = 0;
    }

    void test() {
        Constructors mutable = new Constructors();
        mutable.field = 0;

        Constructors immutable = new Constructors(4);
        immutable.field = 4;
    }
}
