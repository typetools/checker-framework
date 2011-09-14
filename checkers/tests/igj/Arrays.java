import checkers.igj.quals.*;

public class Arrays {

    public void testMutableArrays() {
        String[] s = new String[3];
        String @Immutable [] m = s;
    }

    public void testMutate() {
        String @Mutable [] m = (String @Mutable [])null;
        m[3] = "m";

        String @ReadOnly [] ro = (String @ReadOnly [])null;
        ro[0] = "m";    // error

        String @Immutable [] im = (String @Immutable [] )null;
        im[3] = "n";    // error
    }
}
