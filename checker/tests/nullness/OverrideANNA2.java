import org.checkerframework.checker.initialization.qual.*;
import org.checkerframework.checker.nullness.qual.*;

public class OverrideANNA2 {
    static class Super {
        Object f;

        @EnsuresNonNull("f") // Super.f must be non-null
        void setf(@UnknownInitialization Super this) {
            f = new Object();
        }

        Super() {
            setf();
        }
    }

    static class Sub extends Super {
        Object f; // This shadows super.f

        @Override
        @EnsuresNonNull("f")
        // We cannot ensure that Super.f is non-null since it is
        // shadowed by Sub.f, hence we get an error.
        // :: error: (contracts.postcondition.override.invalid)
        void setf(@UnknownInitialization Sub this) {
            f = new Object();
        }

        Sub() {
            setf();
        }
    }

    public static void main(String[] args) {
        Super s = new Sub();
        s.f.hashCode();
    }
}
