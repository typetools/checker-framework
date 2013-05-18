import checkers.initialization.quals.*;
import checkers.nullness.quals.*;

import checkers.nullness.quals.EnsuresNonNull;

public class PostconditionBug {
    

    void a(@UnknownInitialization @Raw PostconditionBug this) {
         @NonNull String f = "abc";
         //:: error: (assignment.type.incompatible)
         f = null;
     }
}
