// @skip-test
import org.checkerframework.checker.tainting.qual.*;

class Issue2159 {
    void testPolyTaintedLocal(@PolyTainted Object input) {
        //        @PolyTainted Object local = new @PolyTainted Object();
        //        @PolyTainted Object local1 = (@PolyTainted Object) new Object();

        @Untainted Object local = new @Untainted Object();
        @Untainted Object local1 = (@Untainted Object) new Object();
    }
}
