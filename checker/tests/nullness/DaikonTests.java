import org.checkerframework.checker.nullness.qual.*;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;

/*
 * Miscellaneous tests based on problems found when checking Daikon.
 */
public class DaikonTests {

    // Based on a problem found in PPtSlice.
    class Bug1 {
        @Nullable Object field;

        public void cond1() {
            if (this.hashCode() > 6 && Bug1Other.field != null) {
                // spurious dereference error
                Bug1Other.field.toString();
            }
        }

        public void cond1(Bug1 p) {
            if (this.hashCode() > 6 && p.field != null) {
                // works
                p.field.toString();
            }
        }

        public void cond2() {
            if (Bug1Other.field != null && this.hashCode() > 6) {
                // works
                Bug1Other.field.toString();
            }
        }
    }

    // Based on problem found in PptCombined.
    // Not yet able to reproduce the problem :-(

    class Bug2Data {
        Bug2Data(Bug2Super o) {}
    }

    class Bug2Super {
        public @MonotonicNonNull Bug2Data field;
    }

    class Bug2 extends Bug2Super {
        private void m() {
            field = new Bug2Data(this);
            field.hashCode();
        }
    }

    // Based on problem found in FloatEqual.
    class Bug3 {
        @EnsuresNonNullIf(expression = "derived", result = true)
        public boolean isDerived() {
            return (derived != null);
        }

        @Nullable Object derived;

        void good1(Bug3 v1) {
            if (!v1.isDerived() || !(5 > 9)) {
                return;
            }
            v1.derived.hashCode();
        }

        // TODO: this is currently not supported
        //        void good2(Bug3 v1) {
        //            if (!(v1.isDerived() && (5 > 9)))
        //                return;
        //            v1.derived.hashCode();
        //        }

        void good3(Bug3 v1) {
            if (!v1.isDerived() || !(v1 instanceof Bug3)) {
                return;
            }
            Object o = (Object) v1.derived;
            o.hashCode();
        }
    }

    // Based on problem found in PrintInvariants.
    // Not yet able to reproduce the problem :-(

    class Bug4 {
        @MonotonicNonNull Object field;

        void m(Bug4 p) {
            if (false && p.field != null) {
                p.field.hashCode();
            }
        }
    }

    // Based on problem found in chicory.Runtime:
    class Bug5 {
        @Nullable Object clazz;

        @EnsuresNonNull("clazz")
        void init() {
            clazz = new Object();
        }

        void test(Bug5 b) {
            if (b.clazz == null) {
                b.init();
            }

            // The problem is:
            // In the "then" branch, we have in "nnExpr" that "clazz" is non-null.
            // In the "else" branch, we have in "annos" that the variable is non-null.
            // However, as these are facts in two different representations, the merge keeps
            // neither!
            //
            // no error message expected
            b.clazz.hashCode();
        }
    }

    // From LimitedSizeSet.  The following initialization of the values array
    // has caused a NullPointerException.
    class Bug6<T> {
        protected @Nullable T @Nullable [] values;

        public Bug6() {
            // :: warning: [unchecked] unchecked cast
            @Nullable T[] new_values_array = (@Nullable T[]) new @Nullable Object[4];
            values = new_values_array;
        }
    }
}

class Bug1Other {
    static @Nullable Object field;
}
