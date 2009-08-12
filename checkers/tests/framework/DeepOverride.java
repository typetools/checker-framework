import checkers.util.test.*;
import java.util.*;

public class DeepOverride {
    public static class A {
        public @Odd String method() {
            return null;
        }
    }

    public static class B extends A {

    }

    public static class C extends B {
        @Override
        public String method() {
            return "";
        }
    }
}
