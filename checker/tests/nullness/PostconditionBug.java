import org.checkerframework.checker.initialization.qual.*;
import org.checkerframework.checker.nullness.qual.*;

import org.checkerframework.checker.nullness.qual.EnsuresNonNull;

public class PostconditionBug {
    

    void a(@UnknownInitialization @Raw PostconditionBug this) {
         @NonNull String f = "abc";
         //:: error: (assignment.type.incompatible)
         f = null;
     }
}
