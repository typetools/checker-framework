import checkers.igj.quals.*;

public class Primitives {

    public void testPrimitives() {
        int i1 = 3;
        Integer i2 = i1;

        // This one is a hack to insure that
        // TODO: check again to see if need to be changed
        @Mutable Comparable<?> o1 = i1;
        @Mutable Comparable<?> o2 = i2;

        @Immutable Comparable<?> o3 = i1;
        @Immutable Comparable<?> o4 = i2;
    }
}