import checkers.util.test.*;
import java.util.*;

// TODO: the output have a "missing return statement"?
public class DeepOverrideBug {

    public static interface I {
        @Odd String interfaceMethod();
    String abstractMethod();
    }

    public static abstract class A {
        public abstract @Odd String abstractMethod();
    public abstract String interfaceMethod();
    }

    public static abstract class B extends A implements I {

    }

    public static class C extends B {
        public String interfaceMethod() {  // should emit error
        return null;
        }
        public String abstractMethod() { // should emit error
        return null;
        }
    }

}
