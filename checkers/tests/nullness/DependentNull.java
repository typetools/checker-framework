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
        //:: (type.incompatible)
        t1.dep = null;          // error
        t1.dep = "m";
        //:: (type.incompatible)
        t1.indep = null;        // error
        t1.indep = "m";

        @Prototype DependentNull t2 = new DependentNull();
        t2.dep = null;
        t2.dep = "m";
        //:: (type.incompatible)
        t2.indep = null;        // error
        t2.indep = "m";
    }

    void receiverNonProto() {
        //:: (type.incompatible)
        dep = null;             // error
        dep = "m";

        //:: (type.incompatible)
        indep = null;           // error
        indep = "m";
    }

    void receiverProto() @Prototype {
        // dep = null;   FIXME
        dep = "m";

        //:: (type.incompatible)
        indep = null;           // error
        indep = "m";
    }

    class Parameter {
        Parameter(@Dependent(result = Nullable.class, when = Prototype.class) String param) {
        }

        void use() {
            new @Prototype Parameter(null);
            //:: (type.incompatible)
            new Parameter(null); // error

            new @Prototype Parameter("m");
            new Parameter("m");
        }
    }

}
