/*
 * @test
 * @summary Test that java.lang annotations can be used.
 * @library .
 * @compile/fail/ref=Driver.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -Astubs=MyStub.astub Driver.java  -AstubWarnIfNotFound -Werror
 */

class Driver {
    void test() {
        Object o = null;
        String v = String.valueOf(o);
    }
}
