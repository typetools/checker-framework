/*
 * @test
 * @summary Test that the stub files get invoked
 * @library .
 * @compile -XDrawDiagnostics -processor checkers.nullness.NullnessChecker -Astubs=sample.astub Sample.java
 */

class Sample {
    void test() {
        Object o = null;
        String v = String.valueOf(o);
    }
}
