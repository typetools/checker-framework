import checkers.nullness.quals.*;
import checkers.initialization.quals.*;

public class TypeFrames {
    
    class A {
        @NonNull String a;
        
        public A() {
            @UnderInitializion A l1 = this;
            //:: error: (assignment.type.incompatible)
            @UnderInitializion(A.class) A l2 = this;
            a = "";
            @UnderInitializion(A.class) A l3 = this;
        }
    }
    
    interface I {}
    class B extends A implements I {
        @NonNull String b;
        
        public B() {
            super();
            @UnderInitializion(A.class) A l1 = this;
            //:: error: (assignment.type.incompatible)
            @UnderInitializion(B.class) A l2 = this;
            b = "";
            @UnderInitializion(B.class) A l3 = this;
        }
    }
    
    // subtyping
    void t1 (@UnderInitializion(A.class) B b1, @UnderInitializion(B.class) B b2) {
        @UnderInitializion(A.class) B l1 = b1;
        @UnderInitializion(A.class) B l2 = b2;
        //:: error: (assignment.type.incompatible)
        @UnderInitializion(B.class) B l3 = b1;
    }
}
