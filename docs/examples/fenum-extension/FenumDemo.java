import org.checkerframework.checker.fenum.qual.Fenum;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.framework.qual.DefaultQualifier;

import qual.MyFenum;

@SuppressWarnings("fenum:assignment.type.incompatible") // initialization of fake enums
class TestStatic {
    public static final @Fenum("A") int ACONST1 = 1;
    public static final @Fenum("A") int ACONST2 = 2;

    public static final @Fenum("B") int BCONST1 = 4;
    public static final @Fenum("B") int BCONST2 = 5;

    public static final @MyFenum int CCONST1 = 5;
    public static final @MyFenum int CCONST2 = 6;
}

public class FenumDemo {
    @Fenum("A") int state1 = TestStatic.ACONST1; // ok

    @Fenum("B") int state2 = TestStatic.ACONST1; // Incompatible fenums forbidden!

    @MyFenum int state3 = TestStatic.CCONST1; // ok

    short state4 = TestStatic.CCONST1; // ok, @MyFenum applies to short by default
    @NonNegative short state5 = TestStatic.CCONST1; // ok, @MyFenum also applies to
    // @NonNegative short by default (see issue #333)

    int state6 = TestStatic.BCONST1; // Incompatible fenums forbidden!
    @NonNegative int state7 = TestStatic.BCONST1; // Incompatible fenums forbidden!

    void fenumArg(@Fenum("A") int p) {}

    void myFenumArg(@MyFenum int p) {}

    void foo() {
        state1 = 4; // Direct use of value forbidden!
        state1 = TestStatic.BCONST1; // Incompatible fenums forbidden!
        state1 = TestStatic.ACONST2; // ok

        fenumArg(5); // Direct use of value forbidden!
        fenumArg(TestStatic.BCONST1); // Incompatible fenums forbidden!
        fenumArg(TestStatic.ACONST1); // ok

        state3 = 8;
        state3 = TestStatic.ACONST2; // Incompatible fenums forbidden!
        state3 = TestStatic.CCONST2; // ok

        myFenumArg(8); // Direct use of value forbidden!
        myFenumArg(TestStatic.BCONST2); // Incompatible fenums forbidden!
        myFenumArg(TestStatic.CCONST1); // ok
    }

    @DefaultQualifier(MyFenum.class)
    void bar() {
        int int0 = TestStatic.CCONST1; // ok, @MyFenum applies by default in this method
        @NonNegative int int1 = TestStatic.CCONST1; // ok, @MyFenum applies by default in this method
    }

    void comparisons() {
        if (TestStatic.ACONST1 < TestStatic.ACONST2) {
            // ok
        }
        if (TestStatic.CCONST1 > TestStatic.CCONST2) {
            // ok
        }

        // :: error: (binary.type.incompatible)
        if (TestStatic.ACONST1 < TestStatic.BCONST2) {}
        // :: error: (binary.type.incompatible)
        if (TestStatic.ACONST1 == TestStatic.BCONST2) {}
        // :: error: (binary.type.incompatible)
        if (TestStatic.ACONST1 >= TestStatic.CCONST2) {}

        // :: error: (binary.type.incompatible)
        if (TestStatic.ACONST1 < 5) {}
        // :: error: (binary.type.incompatible)
        if (TestStatic.BCONST1 > 5) {}
        // :: error: (binary.type.incompatible)
        if (TestStatic.CCONST1 == 5) {}
    }
}
