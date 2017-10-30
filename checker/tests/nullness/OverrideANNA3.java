import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.checker.nullness.qual.Raw;

class OverrideANNA3 {
    static class Super {
        Object f;
        Object g;

        @EnsuresNonNull({"f", "g"})
        void setfg(@Raw @UnknownInitialization Super this) {
            f = new Object();
            g = new Object();
        }

        Super() {
            setfg();
        }
    }

    static class Sub extends Super {
        @Override
        @EnsuresNonNull("f")
        // :: error: (contracts.postcondition.override.invalid)
        void setfg(@Raw @UnknownInitialization Sub this) {
            f = new Object();
        }
    }

    public static void main(String[] args) {
        Super s = new Sub();
        s.g.hashCode();
    }
}
