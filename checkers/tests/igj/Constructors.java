import checkers.igj.quals.*;

public class Constructors {
    int field;

    Constructors() {
        field = 0;
    }

    @Immutable
    Constructors(int a) {
        field = 0;
    }

    void test() {
        Constructors mutable = new Constructors();
        mutable.field = 0;

        //TODO: This is a bug see issue
        //:: error: (constructor.invocation.invalid)
        Constructors immutable = new Constructors(4);
        //:: error: (assignability.invalid)
        immutable.field = 4;
    }
}
