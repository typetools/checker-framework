import checkers.util.test.*;

import java.util.*;
import checkers.quals.*;

// various tests for the @Pure annotation
class Purity {
    
    String f1, f2, f3;
    
    // a method that is not pure
    void nonpure() {
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
    
}
