import checkers.nullness.quals.*;
import java.util.*;

class RawAssertNonNull {
    class Test1 {
        private Object f;
        private Object g;

        Test1() {
            m();
        }

        @AssertNonNullAfter({"f", "g"})
        private void m(@Raw Test1 this) {
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

        @AssertNonNullAfter({"f", "g"})
        //:: error: (assert.postcondition.not.satisfied)
        private void m(@Raw Test1b this) {
            this.f = new Object();
        }
    }

    class Test1c {
        private Object f;
        private Object g;

        //:: error: (fields.uninitialized)
        Test1c() {
            m();
        }

        @AssertNonNullAfter({"f"})
        private void m(@Raw Test1c this) {
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
            assert this.g!=null : "nullness assumption";
        }

        @AssertNonNullAfter({"f"})
        private void m(@Raw Test1d this) {
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

        @AssertNonNullAfter({"f", "g"})
        private void m(@Raw Test2 this, Global g) {
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

        @AssertNonNullAfter({"f", "g"})
        private void m(@Raw Test2b this) {
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

        @AssertNonNullAfter({"f", "g"})
        private void m(@Raw Test2c this) {
            this.f = new ArrayList<String>();
            this.g = new ArrayList<String>();
        }

        private void nonpure(@Raw Test2c this) {
            //:: error: (assignment.type.incompatible)
            this.f = null;
        }
    }

    class Global {
        void nonpure() {}
    }
}