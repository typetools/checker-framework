import tests.util.*;
import java.util.*;

public class DeepOverrideInterface {

    public static interface I {
        @Odd String interfaceMethod();
    }

    public static abstract class A {
        public abstract @Odd String abstractMethod();
    }

    public static abstract class B extends A implements I {

    }

    public static class C extends B {
        //:: error: (override.return.invalid)
        public String interfaceMethod() {
            return "";
        }
        public @Odd String abstractMethod() {
            return null;
        }
    }

}
