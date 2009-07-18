import checkers.util.test.*;
import java.util.*;

public class DeepOverrideAbstract {

    public static interface I {
        @Odd String interfaceMethod();
    }

    public static abstract class A {
        public abstract @Odd String abstractMethod();
    }

    public static abstract class B extends A implements I {

    }

    public static class C extends B {
        public @Odd String interfaceMethod() {
            return null;
        }
        public String abstractMethod() {
            return "";
        }
    }

}
