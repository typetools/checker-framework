import checkers.nullness.quals.*;
import checkers.initialization.quals.*;

public class TypeFrames {
    
    class A {
        @NonNull String a;
        
        public A() {
            @Free A l1 = this;
            //:: error: (assignment.type.incompatible)
            @Free(A.class) A l2 = this;
            a = "";
            @Free(A.class) A l3 = this;
        }
    }
    
    interface I {}
    class B extends A implements I {
        @NonNull String b;
        
        public B() {
            super();
            @Free(A.class) A l1 = this;
            //:: error: (assignment.type.incompatible)
            @Free(B.class) A l2 = this;
            b = "";
            @Free(B.class) A l3 = this;
        }
    }
    
    // subtyping
    void t1 (@Free(A.class) B b1, @Free(B.class) B b2) {
        @Free(A.class) B l1 = b1;
        @Free(A.class) B l2 = b2;
        //:: error: (assignment.type.incompatible)
        @Free(B.class) B l3 = b1;
    }
}
