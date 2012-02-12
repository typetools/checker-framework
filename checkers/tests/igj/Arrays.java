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
        String @Mutable [] m = (String @Mutable [])null;
        m[3] = "m";

        String @ReadOnly [] ro = (String @ReadOnly [])null;
        //:: error: (assignability.invalid)
        ro[0] = "m";

        String @Immutable [] im = (String @Immutable [] )null;
        //:: error: (assignability.invalid)
        im[3] = "n";
    }
}
