package examples;

import checkers.initialization.quals.Unclassified;
import checkers.nullness.quals.NonNull;


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
