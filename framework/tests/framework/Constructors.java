import org.checkerframework.framework.testchecker.util.Odd;

public class Constructors {
    public Constructors(Constructors con) {}

    public void testConstructors() {
        Constructors c = null;
        // :: warning: (cast.unsafe.constructor.invocation)
        new @Odd Constructors(c);
    }

    // Test anonymous constructors
    public Constructors(@Odd String s, int i) {}

    public void testStaticAnonymousConstructor() {
        String notOdd = "m";

        // :: error: (argument.type.incompatible)
        new Constructors(notOdd, 0); // error
        // :: error: (argument.type.incompatible)
        new Constructors(notOdd, 0) {}; // error
    }

    private class MyConstructors extends Constructors {
        public MyConstructors(@Odd String s) {
            super(s, 0);
        }
    }

    public static void testAnonymousConstructor() {
        Constructors m = new Constructors(null) {};
        String notOdd = "m";
        // :: error: (argument.type.incompatible)
        m.new MyConstructors(notOdd); // error
        // :: error: (argument.type.incompatible)
        m.new MyConstructors(notOdd) {}; // error
    }

    // Tests that should pass
    public void testPassingTests() {
        @Odd String odd = null;

        new Constructors(odd, 0);
        new Constructors(odd, 0) {};

        Constructors m = new Constructors(null) {};
        m.new MyConstructors(odd);
        m.new MyConstructors(odd) {};
    }
}
