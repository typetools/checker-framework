import checkers.util.test.*;

import java.util.*;
import checkers.quals.*;
import tests.util.*;
import checkers.quals.Pure.Kind;

// various tests for the checker to automatically suggest pure methods (most methods have been copied from Purity.java)
//:: warning: (pure.more.pure)
class Test {
    
    String f1, f2, f3;
    String[] a;
    
    // class with a (potentially) non-pure constructor
    private static class NonPureClass {
        String t;
        public NonPureClass() {
            t = "";
        }
    }
    
    // class with a pure constructor
    private static class PureClass {
        //:: warning: (pure.more.pure)
        public PureClass() {
        }
    }
    
    //:: warning: (pure.more.pure)
    void nonpure() {
    }
    
    @Pure
    String pure() {
        return "";
    }
    
    String t3() {
      nonpure();
      return "";
    }
    
    //:: warning: (pure.more.pure)
    String t4() {
        pure();
        return "";
    }
    
    //:: warning: (pure.more.pure)
    int t5() {
        int i = 1;
        return i;
    }
    
    //:: warning: (pure.more.pure)
    int t6() {
        int j = 0;
        for (int i = 0; i < 10; i++) {
            j = j - i;
        }
        return j;
    }
    
    //:: warning: (pure.more.pure)
    String t7() {
        if (true) {
            return "a";
        }
        return "";
    }
    
    //:: warning: (pure.more.pure)
    int t8() {
        return 1 - 2 / 3 * 2 % 2;
    }
    
    //:: warning: (pure.more.pure)
    String t9() {
        return "b" + "a";
    }
    
    String t10() {
        f1 = "";
        f2 = "";
        return "";
    }
    
    String t11(Test l) {
        l.a[0] = "";
        return "";
    }
    
    String t12(String[] s) {
        s[0] = "";
        return "";
    }
    
    String t13() {
        PureClass p = new PureClass();
        return "";
    }
    
    String t13b() {
        PureClass p = new PureClass();
        return "";
    }
    
    String t13c() {
        PureClass p = new PureClass();
        return "";
    }
    
    //:: warning: (pure.more.pure)
    String t14() {
        String i = "";
        i = "a";
        return i;
    }
    
    //:: warning: (pure.more.pure)
    String t15() {
        String[] s = new String[1];
        return s[0];
    }
    
    //:: warning: (pure.more.pure)
    String t16() {
        try {
            int i = 1/0;
        } catch (Throwable t) {
            // ..
        }
        return "";
    }
    
    //:: warning: (pure.more.pure)
    String t16b() {
        try {
            int i = 1/0;
        } catch (Throwable t) {
            // ..
        }
        return "";
    }
    
    //:: warning: (pure.more.pure)
    String t16c() {
        try {
            int i = 1/0;
        } catch (Throwable t) {
            // ..
        }
        return "";
    }
    
    //:: warning: (pure.more.pure)
    String t17() {
        return "";
    }
    
    String t12() {
        NonPureClass p = new NonPureClass();
        return "";
    }
}
