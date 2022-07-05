import org.checkerframework.checker.regex.qual.Regex;

public class Constructors {
    public Constructors(Constructors con) {}

    public Constructors(@Regex String s, int i) {}

    public class MyConstructors extends Constructors {
        public MyConstructors(@Regex String s) {
            super(s, 0);
        }
    }

    public void testAnonymousConstructor(String s) {

        Constructors m = new Constructors(null);

        // :: error: (argument.type.incompatible)
        new MyConstructors(s);
        // :: error: (argument.type.incompatible)
        new MyConstructors(s) {};
        // :: error: (argument.type.incompatible)
        m.new MyConstructors(s);
        // :: error: (argument.type.incompatible)
        m.new MyConstructors(s) {};
    }
}
