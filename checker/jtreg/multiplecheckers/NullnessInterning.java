/*
 * @test
 * @summary Test that command-line options can be provided to specific org.checkerframework.checker
 *
 * @compile/ref=NullnessInterning1.out -XDrawDiagnostics -Anomsgtext -processor org.checkerframework.checker.nullness.NullnessChecker,org.checkerframework.checker.interning.InterningChecker NullnessInterning.java -Awarns
 *
 * @compile/ref=NullnessInterning2.out -XDrawDiagnostics -Anomsgtext -processor org.checkerframework.checker.nullness.NullnessChecker,org.checkerframework.checker.interning.InterningChecker NullnessInterning.java -Awarns -ANullnessChecker_skipDefs=NullnessInterning
 *
 * @compile/ref=NullnessInterning2.out -XDrawDiagnostics -Anomsgtext -processor org.checkerframework.checker.nullness.NullnessChecker,org.checkerframework.checker.interning.InterningChecker NullnessInterning.java -Awarns -Aorg.checkerframework.checker.nullness.NullnessChecker_skipDefs=NullnessInterning
 *
 * @compile/ref=NullnessInterning3.out -XDrawDiagnostics -Anomsgtext -processor org.checkerframework.checker.nullness.NullnessChecker,org.checkerframework.checker.interning.InterningChecker NullnessInterning.java -Awarns -AInterningChecker_skipDefs=NullnessInterning
 *
 * @compile/ref=NullnessInterning3.out -XDrawDiagnostics -Anomsgtext -processor org.checkerframework.checker.nullness.NullnessChecker,org.checkerframework.checker.interning.InterningChecker NullnessInterning.java -Awarns -Aorg.checkerframework.checker.interning.InterningChecker_skipDefs=NullnessInterning
 *
 * @compile/ref=NullnessInterning4.out -XDrawDiagnostics -Anomsgtext -processor org.checkerframework.checker.nullness.NullnessChecker,org.checkerframework.checker.interning.InterningChecker NullnessInterning.java -Awarns -Aorg.checkerframework.common.basetype.BaseTypeChecker_skipDefs=NullnessInterning
 */

public class NullnessInterning {
    Object f = null;

    void m(Object p) {
        if (f == p) {}
    }
}
