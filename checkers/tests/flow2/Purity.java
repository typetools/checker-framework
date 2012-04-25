import checkers.util.test.*;

import java.util.*;
import checkers.quals.*;

// various tests for the @Pure annotation
class Purity {
    
    String f1, f2, f3;
    String[] a;
    
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
    
    //:: error: (pure.not.pure)
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
    
    //:: error: (pure.not.pure)
    @Pure String t10() {
        f1 = "";
        f2 = "";
        return "";
    }
    
    //:: error: (pure.not.pure)
    @Pure String t11(Purity l) {
        l.a[0] = "";
        return "";
    }
    
    //:: error: (pure.not.pure)
    @Pure String t12(String[] s) {
        s[0] = "";
        return "";
    }
    
    //:: error: (pure.not.pure)
    @Pure String t13() {
        // could be relaxed in the future to allow certain object creations. one has to be
        // careful about whether the code in the constructor might have side effects.
        String s = new String();
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
    
}
