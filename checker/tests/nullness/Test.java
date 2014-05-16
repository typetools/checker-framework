package examples;

import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.qual.NonNull;


public class Test {
    
    void a() {
        @NonNull String f = "abc";
        
        //:: error: (assignment.type.incompatible)
        f = null;
    }
    
    void b() {
        @UnknownInitialization @NonNull Test f = new Test();
    }
}
