import org.checkerframework.checker.igj.qual.*;

//This test is skipped because the IGJ/OIGJ org.checkerframework.checker are not fully compatible with the latest
//version of the Checker Framework.
//See issue http://code.google.com/p/checker-framework/issues/detail?id=199.
//@skip-test

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
        Constructors immutable = new Constructors(4);
        //:: error: (assignability.invalid)
        immutable.field = 4;
    }
}
