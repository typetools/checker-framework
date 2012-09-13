package examples;

import checkers.initialization.quals.Unclassified;
import checkers.nonnull.quals.NonNull;


public class Test {
    
    void a() {
        @NonNull String f = "abc";
        
        //:: error: (assignment.type.incompatible)
        f = null;
    }
    
    void b() {
        @Unclassified @NonNull Test f = new Test();
    }
}
