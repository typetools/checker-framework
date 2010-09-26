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
        //:: (assignment.type.incompatible)
        t1.dep = null;          // error
        t1.dep = "m";
        //:: (assignment.type.incompatible)
        t1.indep = null;        // error
        t1.indep = "m";

        @Prototype DependentNull t2 = new DependentNull();
        t2.dep = null;
        t2.dep = "m";
        //:: (assignment.type.incompatible)
        t2.indep = null;        // error
        t2.indep = "m";
    }

    void receiverNonProto() {
        //:: (assignment.type.incompatible)
        dep = null;             // error
        dep = "m";

        //:: (assignment.type.incompatible)
        indep = null;           // error
        indep = "m";
    }

    void receiverProto() @Prototype {
        // dep = null;   FIXME
        dep = "m";

        //:: (assignment.type.incompatible)
        indep = null;           // error
        indep = "m";
    }

    class Parameter {
        Parameter(@Dependent(result = Nullable.class, when = Prototype.class) String param) {
        }

        void use() {
            new @Prototype Parameter(null);
            //:: (argument.type.incompatible)
            new Parameter(null); // error

            new @Prototype Parameter("m");
            new Parameter("m");
        }
    }

}
