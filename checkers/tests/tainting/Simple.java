import checkers.tainting.quals.*;

class Simple {

    void execute(@Untainted String s) { }
    void tainted(String s) { }

    void stringLiteral() {
        execute("ldskjfldj");
        tainted("lksjdflkjdf");
    }

    void stringRef(String ref) {
        execute(ref);   // error
        tainted(ref);
    }

    void untaintedRef(@Untainted String ref) {
        execute(ref);
        tainted(ref);
    }

    void concatenation(@Untainted String s1, String s2) {
        execute(s1 + s1);
        execute(s1 + "m");
        execute(s1 + s2);   // error

        execute(s2 + s1);   // error
        execute(s2 + "m");  // error
        execute(s2 + s2);   // error

        tainted(s1 + s1);
        tainted(s1 + "m");
        tainted(s1 + s2);

        tainted(s2 + s1);
        tainted(s2 + "m");
        tainted(s2 + s2);

    }
}