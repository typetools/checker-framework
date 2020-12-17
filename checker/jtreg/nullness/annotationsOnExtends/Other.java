/*
 * @test
 * @summary Test for bug when storing annotations on extends or implements in class declarations in elements.
 *
 * @compile -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker Test.java Test2.java Other.java
 * @compile -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker Other.java Test.java Test2.java
 */

public class Other {
    void foo() {
        Test other = null;
        Test2 other2 = null;
    }
}
