import checkers.nullness.quals.*;

/*
 * Miscellaneous tests based on problems found when checking Daikon.
 */
public class DaikonTests {

    // Based on a problem found in PPtSlice.
    class Bug1 {
        @Nullable Object field;

        // skip-test TODO
        /*
        public void cond1() {
            if ( this.hashCode() > 6 && Bug1Other.field != null ) {
                // spurious dereference error
                Bug1Other.field.toString();
            }
        }
        */
        public void cond1(Bug1 p) {
            if ( this.hashCode() > 6 && p.field != null ) {
                // works
                p.field.toString();
            }
        }

        public void cond2() {
            if ( Bug1Other.field != null && this.hashCode() > 6 ) {
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
        public @LazyNonNull Bug2Data field;
    }
    
    class Bug2 extends Bug2Super {
        private void m() {
            field = new Bug2Data(this);
            field.hashCode();
        }
    }
    
    // Based on problem found in FloatEqual.
    class Bug3 {
        /*@AssertNonNullIfTrue("derived")*/
        public boolean isDerived() {
            return (derived != null);
        }
        @Nullable Object derived;
        
        void good1(Bug3 v1) {
            if (!v1.isDerived() || !(5 > 9))
                return;
            v1.derived.hashCode();
        }
        
        void good2(Bug3 v1) {
            if (!(v1.isDerived() && (5 > 9)))
                return;
            v1.derived.hashCode();
        }
        
        void good3(Bug3 v1) {
            if (!v1.isDerived() || !(v1 instanceof Bug3))
                return;
            Object o = (Object) v1.derived;
            o.hashCode();
        }
    }
 
    // Based on problem found in PrintInvariants.
    // Not yet able to reproduce the problem :-(

    class Bug4 {
        @LazyNonNull Object field;
        
        void m(Bug4 p) {
            if (false && p.field != null)
                p.field.hashCode();
        }
    }
    
    // Based on problem found in chicory.Runtime:
    
    // skip-test TODO
    /*
    class Bug5 {
        @Nullable Object clazz;
        
        @AssertNonNullAfter("clazz")
        void init() {
            clazz = new Object();
        }
        
        void test(Bug5 b) {
            if (b.clazz == null)
                b.init();

            // The problem is:
            // In the "then" branch, we have in "nnExpr" that "clazz" is non-null.
            // In the "else" branch, we have in "annos" that the variable is non-null.
            // However, as these are facts in two different representations, the merge keeps neither!
            //
            // no error message expected
            b.clazz.hashCode();
        }
    }
    */
}

class Bug1Other {
    static @Nullable Object field;
}
