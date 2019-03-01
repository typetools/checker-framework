import org.checkerframework.checker.tainting.qual.*;

class Issue2159 {
    Issue2159() {}

    @PolyTainted Issue2159(@PolyTainted Object x) {}

    void testPolyTaintedLocal(@PolyTainted Object input) {
        @PolyTainted Object local = (@PolyTainted Issue2159) new Issue2159();
        @PolyTainted Object local1 = new @PolyTainted Issue2159();
    }
}
