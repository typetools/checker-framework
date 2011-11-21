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
        private void m() @Raw {
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
        private void m() @Raw {
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
        private void m() @Raw {
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
        //:: error: (assert.postcondition.not.satisfied)
        private void m(Global g) @Raw {
            this.f = new ArrayList<String>();
            this.g = new ArrayList<String>();
            g.nonpure();
            // The call to the non-pure method erases all knowledge
            // about fields -> assertion not satisfied.
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
        private void m() @Raw {
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
        private void m() @Raw {
            this.f = new ArrayList<String>();
            this.g = new ArrayList<String>();
        }

        private void nonpure() @Raw {
            //:: error: (assignment.type.incompatible)
            this.f = null;
        }
    }

    class Global {
        void nonpure() {}
    }
}