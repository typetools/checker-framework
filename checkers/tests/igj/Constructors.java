import checkers.igj.quals.*;

public class Constructors {
    int field;

    Constructors(@Mutable Constructors this) {
        field = 0;
    }

    // TODO: the annotation on the constructor wasn't previously
    // necessary. However, without it the constructor instantiation
    // below fails. Should a receiver annotation be automatically
    // propagated?
    // TODO JSR 308: is this receiver annotation even still valid?
    @Immutable 
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