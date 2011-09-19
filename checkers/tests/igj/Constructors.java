import checkers.igj.quals.*;

public class Constructors {
    int field;

    @Mutable Constructors() {
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
