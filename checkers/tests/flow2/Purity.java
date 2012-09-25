import checkers.util.test.*;

import java.util.*;
import checkers.quals.*;
import tests.util.*;
import checkers.quals.Pure.Kind;

// various tests for the @Pure annotation
class Purity {
    
    String f1, f2, f3;
    String[] a;
    
    // class with a (potentially) non-pure constructor
    private static class NonPureClass {
    }
    
    // class with a pure constructor
    private static class PureClass {
        @Pure(Kind.SIDE_EFFECT_FREE)
        public PureClass() {
        }
    }
    
    // class with wrong purity annotations on constructors
    private static class InvalidClass {
        @Pure(Kind.DETERMINISTIC)
        //:: error: (pure.determinstic.constructor)
        public InvalidClass() {
        }
        @Pure
        //:: error: (pure.determinstic.constructor)
        public InvalidClass(int i) {
        }
    }
    
    // a method that is not pure (no annotation)
    void nonpure() {
    }
    
    @Pure String pure() {
        return "";
    }
    
    //:: warning: (pure.void.method)
    @Pure void t1() {
    }
    
    @Pure String t2() {
        return "";
    }
    
    //:: error: (pure.not.deterministic.and.sideeffect.free)
    @Pure String t3() {
      nonpure();
      return "";
    }
    
    @Pure String t4() {
        pure();
        return "";
    }
    
    @Pure int t5() {
        int i = 1;
        return i;
    }
    
    @Pure int t6() {
        int j = 0;
        for (int i = 0; i < 10; i++) {
            j = j - i;
        }
        return j;
    }
    
    @Pure String t7() {
        if (true) {
            return "a";
        }
        return "";
    }
    
    @Pure int t8() {
        return 1 - 2 / 3 * 2 % 2;
    }
    
    @Pure String t9() {
        return "b" + "a";
    }
    
    //:: error: (pure.not.deterministic.and.sideeffect.free)
    @Pure String t10() {
        f1 = "";
        f2 = "";
        return "";
    }
    
    //:: error: (pure.not.deterministic.and.sideeffect.free)
    @Pure String t11(Purity l) {
        l.a[0] = "";
        return "";
    }
    
    //:: error: (pure.not.deterministic.and.sideeffect.free)
    @Pure String t12(String[] s) {
        s[0] = "";
        return "";
    }
    
    //:: error: (pure.not.deterministic)
    @Pure String t13() {
        PureClass p = new PureClass();
        return "";
    }
    
    @Pure(Kind.SIDE_EFFECT_FREE) String t13b() {
        PureClass p = new PureClass();
        return "";
    }
    
    //:: error: (pure.not.deterministic)
    @Pure(Kind.DETERMINISTIC) String t13c() {
        PureClass p = new PureClass();
        return "";
    }
    
    @Pure String t14() {
        String i = "";
        i = "a";
        return i;
    }
    
    @Pure String t15() {
        String[] s = new String[1];
        return s[0];
    }

    //:: error: (pure.not.deterministic)
    @Pure String t16() {
        try {
            int i = 1/0;
        } catch (Throwable t) {
            // ..
        }
        return "";
    }
    
    @Pure(Kind.SIDE_EFFECT_FREE) String t16b() {
        try {
            int i = 1/0;
        } catch (Throwable t) {
            // ..
        }
        return "";
    }
    
    //:: error: (pure.not.deterministic)
    @Pure(Kind.DETERMINISTIC) String t16c() {
        try {
            int i = 1/0;
        } catch (Throwable t) {
            // ..
        }
        return "";
    }
    
    //:: warning: (pure.annotation.with.emtpy.kind)
    @Pure({}) String t17() {
        return "";
    }
    
    //:: error: (pure.not.deterministic.and.sideeffect.free)
    @Pure String t12() {
        NonPureClass p = new NonPureClass();
        return "";
    }
}
