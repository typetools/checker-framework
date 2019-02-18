import org.checkerframework.checker.tainting.qual.*;

class Issue2159 {
    Issue2159() {}

    @Untainted Issue2159(int x) {}

    @PolyTainted Issue2159(@PolyTainted String x) {}

    void testPolyTaintedLocal(@PolyTainted Object input, @Tainted String y) {
        @PolyTainted Object local = new @PolyTainted Object();
        @PolyTainted Issue2159 polyLocal = new @PolyTainted Issue2159("asdf");
        @PolyTainted Issue2159 polyLocal1 = new @PolyTainted Issue2159(y);
        @Untainted Issue2159 unt = new Issue2159(5);
    }

    @PolyTainted Object testMethod() {
        return new @PolyTainted Object();
    }
}
