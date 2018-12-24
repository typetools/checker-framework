import org.checkerframework.checker.tainting.qual.*;

class Issue2159 {
    Issue2159() {}

    void testPolyTaintedLocal(@PolyTainted Object input) {
        @PolyTainted Object local = new @PolyTainted Object();
    }

    @PolyTainted Object testMethod() {
        return new @PolyTainted Object();
    }
}
