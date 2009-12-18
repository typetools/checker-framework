import checkers.nullness.quals.*;
import checkers.quals.*;

public class DependentNull {

    /**
     * NOTE that @Prototype is a SUPERTYPE of an unannotated reference.
     * (Uh, how does the checker know that?  It's important to the checking!)
     */
    @interface Prototype {}


    private @NonNull @Dependent(result = Nullable.class, when=Prototype.class) String dep;
    @NonNull String indep;

    static void fieldAccess() {
        DependentNull t1 = new DependentNull();
        t1.dep = null;          // error
        t1.dep = "m";
        t1.indep = null;        // error
        t1.indep = "m";

        @Prototype DependentNull t2 = new DependentNull();
        t2.dep = null;
        t2.dep = "m";
        t2.indep = null;        // error
        t2.indep = "m";
    }

    void receiverNonProto() {
        dep = null;             // error
        dep = "m";

        indep = null;           // error
        indep = "m";
    }

    void receiverProto() @Prototype {
        // dep = null;   FIXME
        dep = "m";

        indep = null;           // error
        indep = "m";
    }

    class Parameter {
        Parameter(@Dependent(result = Nullable.class, when = Prototype.class) String param) {
        }

        void use() {
            new @Prototype Parameter(null);
            new Parameter(null); // error

            new @Prototype Parameter("m");
            new Parameter("m");
        }
    }

}
