import checkers.initialization.quals.*;
import checkers.nonnull.quals.*;

import checkers.nonnull.quals.EnsuresNonNull;

public class PostconditionBug {
    

    void a(@Unclassified @Raw PostconditionBug this) {
         @NonNull String f = "abc";
         //:: error: (assignment.type.incompatible)
         f = null;
     }
}
