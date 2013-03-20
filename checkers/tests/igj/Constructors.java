import checkers.igj.quals.*;

//This test is skipped because the IGJ/OIGJ checkers are not fully compatible with the latest
//version of the Checker Framework.
//See issue http://code.google.com/p/checker-framework/issues/detail?id=199.
//@skip-test

public class Constructors {
    int field;

    Constructors() {
        field = 0;
    }

    // TODO: the annotation on the constructor wasn't previously
    // necessary. However, without it the constructor instantiation
    // below fails. Should a receiver annotation be automatically
    // propagated?
    // TODO JSR 308: is this receiver annotation even still valid?
    @Immutable 
    Constructors(int a) {
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
