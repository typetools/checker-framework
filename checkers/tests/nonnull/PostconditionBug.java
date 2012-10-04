import checkers.initialization.quals.Unclassified;
import checkers.nonnull.quals.NonNull;

import checkers.nonnull.quals.EnsuresNonNull;

public class PostconditionBug {
    
    @NonNull String c; 

    public PostconditionBug() {
        super();
        b();
   }

    void a(@Unclassified PostconditionBug this) {
         @NonNull String f = "abc";
         
         //:: error: (assignment.type.incompatible)
         f = null;
     }
     
    @EnsuresNonNull("c")
    void b(@Unclassified PostconditionBug this) {
        c = "c";
        a();
    }
}
