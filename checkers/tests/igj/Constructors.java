import checkers.igj.quals.*;

public class Constructors {
    int field;

    Constructors(@Mutable Constructors this) {
        field = 0;
    }

    Constructors(@Immutable Constructors this, int a) {
        field = 0;
    }

    void test() {
        Constructors mutable = new Constructors();
        mutable.field = 0;

        Constructors immutable = new Constructors(4);
        //:: error: (assignability.invalid)
        immutable.field = 4;
    }
}