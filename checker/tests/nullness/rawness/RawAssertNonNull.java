import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.qual.*;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;

class RawAssertNonNull {
    class Test1 {
        private Object f;
        private Object g;

        Test1() {
            m();
        }

        @EnsuresNonNull({"f", "g"})
        private void m(@Raw @UnknownInitialization Test1 this) {
            this.f = new Object();
            this.g = new Object();
        }
    }

    class Test1b {
        private Object f;
        private Object g;

        Test1b() {
            m();
        }

        @EnsuresNonNull({"f", "g"})
        // :: error: (contracts.postcondition.not.satisfied)
        private void m(@Raw @UnknownInitialization Test1b this) {
            this.f = new Object();
        }
    }

    class Test1c {
        private Object f;
        private Object g;

        // :: error: (initialization.fields.uninitialized)
        Test1c() {
            m();
        }

        @EnsuresNonNull({"f"})
        private void m(@Raw @UnknownInitialization Test1c this) {
            this.f = new Object();
            this.g = new Object();
        }
    }

    class Test1d {
        private Object f;
        private Object g;

        Test1d() {
            m();
            // If one has some additional information that the type system hasn't
            // one can suppress the error from Test1c using an assertion,
            // which is nicer than suppressing the warning.
            assert this.g != null : "@AssumeAssertion(nullness)";
        }

        @EnsuresNonNull({"f"})
        private void m(@Raw @UnknownInitialization Test1d this) {
            this.f = new Object();
            this.g = new Object();
        }
    }

    class Test2 {
        private List<String> f;
        private List<String> g;

        Test2(Global g) {
            m(g);
        }

        @EnsuresNonNull({"f", "this.g"})
        private void m(@Raw @UnknownInitialization Test2 this, Global g) {
            this.f = new ArrayList<String>();
            this.g = new ArrayList<String>();
            g.nonpure();
            // The global method sees the fields as non-null and
            // cannot set them to null.
        }
    }

    class Test2b {
        private List<String> f;
        private List<String> g;

        Test2b(Global g) {
            m();
            g.nonpure();
            // The global method sees the fields as non-null and
            // cannot set them to null.
        }

        @EnsuresNonNull({"f", "g"})
        private void m(@Raw @UnknownInitialization Test2b this) {
            this.f = new ArrayList<String>();
            this.g = new ArrayList<String>();
        }
    }

    class Test2c {
        private List<String> f;
        private List<String> g;

        Test2c(Global g) {
            m();
            nonpure();
            // The raw, non-pure method "nonpure" cannot set fields back to null.
            // -> All fields initialized.
        }

        @EnsuresNonNull({"f", "g"})
        private void m(@Raw @UnknownInitialization Test2c this) {
            this.f = new ArrayList<String>();
            this.g = new ArrayList<String>();
        }

        private void nonpure(@Raw @UnknownInitialization Test2c this) {
            // :: error: (assignment.type.incompatible)
            this.f = null;
        }
    }

    class Global {
        void nonpure() {}
    }
}
