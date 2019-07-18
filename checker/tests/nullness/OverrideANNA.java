import org.checkerframework.checker.initialization.qual.*;
import org.checkerframework.checker.nullness.qual.*;

class OverrideANNA {
    static class Super {
        Object f;

        @EnsuresNonNull("f")
        void setf(@UnknownInitialization Super this) {
            f = new Object();
        }

        Super() {
            setf();
        }
    }

    static class Sub extends Super {
        @Override
        // :: error: (contracts.postcondition.not.satisfied)
        void setf(@UnknownInitialization Sub this) {}
    }

    public static void main(String[] args) {
        Super s = new Sub();
        s.f.hashCode();
    }
}
