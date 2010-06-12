import checkers.util.test.*;
import java.util.*;

public class Varargs {
    public void testVarargsInvocation() {
        @Odd String s = null;
        aVarargsMethod(s);
        //:: (argument.type.incompatible)
        aVarargsMethod(s, "");
        aVarargsMethod(s, s);

        moreVarargs(new @Odd String[1]);
        //:: (argument.type.incompatible)
        moreVarargs(new String @Odd [1]);
        moreVarargs(new @Odd String(), new @Odd String());
        //:: (argument.type.incompatible)
        moreVarargs(new String(), new @Odd String());
        //:: (argument.type.incompatible)
        moreVarargs(new String(),
                    //:: (argument.type.incompatible)
                new String());
    }

    /* ------------------------------------------------------------ */

    public void aVarargsMethod(@Odd String s, @Odd String ... more) {
    }

    public void moreVarargs(@Odd String ... args) {
    }

    Varargs(String ...args) { }

    void test() {
        new Varargs("m", "n");
        new Varargs();
    }

    void testVarargsConstructor() {
        new ProcessBuilder("hello");
    }
}
