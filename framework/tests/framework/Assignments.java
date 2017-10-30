import testlib.util.*;

public class Assignments {

    public void testAssignment() {
        @Odd String s;
        @Odd String t;
        s = null;
        t = s;

        String z = "";
        z = s;
    }

    public void testCompound() {
        // :: warning: (cast.unsafe)
        @Odd String s = (@Odd String) "foo";
        // :: warning: (cast.unsafe)
        @Odd String t = (@Odd String) "bar";
        s += t;

        String z = "";
        z += s;
    }

    public void testEnhancedForLoop() {
        // TODO
    }

    public void testMethod() {
        // Nothing to do here.
    }

    public void testMethodInvocation() {
        // TODO anonymous constructor
        // TODO isEnumSuper
        // @see Varargs

        @Odd String s = null;
        methodA(s);
        methodB(s);
    }

    public @Odd String testReturn() {
        @Odd String s = null;
        return s;
    }

    public void testReturnVoid() {
        return;
    }

    public void testVariable() {
        @Odd String s = null;
        // :: warning: (cast.unsafe)
        @Odd String t = (@Odd String) "foo";
        @Odd String u = s;
        String v = s;
    }

    /* ------------------------------------------------------------ */

    public void methodA(@Odd String s) {}

    public void methodB(String s) {}
}
