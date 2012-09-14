import checkers.igj.quals.*;

public class Arrays {

    public void testMutableArrays() {
        String[] s = new String[3];
        // mutable is not a subtype of immutable
        //:: error: (assignment.type.incompatible)
        String @Immutable [] im = s;
        // mutable is a subtype of readonly
        String @ReadOnly [] ro = s;
    }

    public void testMutate() {
        // Warning only with -Alint:cast:strict.
        //TODO:: warning: (cast.unsafe)
        String @Mutable [] m = (String @Mutable [])null;
        m[3] = "m";

        //TODO:: warning: (cast.unsafe)
        String @ReadOnly [] ro = (String @ReadOnly [])null;
        //:: error: (assignability.invalid)
        ro[0] = "m";

        //TODO:: warning: (cast.unsafe)
        String @Immutable [] im = (String @Immutable [] )null;
        //:: error: (assignability.invalid)
        im[3] = "n";
    }
}
