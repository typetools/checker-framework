/*
 * @test
 * @summary Test that command-line options can be provided to specific checkers
 *
 * @compile/ref=NullnessInterning1.out -XDrawDiagnostics -Anomsgtext -processor checkers.nullness.NullnessChecker,checkers.interning.InterningChecker NullnessInterning.java -Awarns
 *
 * @compile/ref=NullnessInterning2.out -AprintErrorStack -XDrawDiagnostics -Anomsgtext -processor checkers.nullness.NullnessChecker,checkers.interning.InterningChecker NullnessInterning.java -Awarns -ANullnessChecker_skipDefs=NullnessInterning
 *
 * @compile/ref=NullnessInterning2.out -XDrawDiagnostics -Anomsgtext -processor checkers.nullness.NullnessChecker,checkers.interning.InterningChecker NullnessInterning.java -Awarns -Acheckers.nullness.NullnessChecker_skipDefs=NullnessInterning
 *
 * @compile/ref=NullnessInterning3.out -XDrawDiagnostics -Anomsgtext -processor checkers.nullness.NullnessChecker,checkers.interning.InterningChecker NullnessInterning.java -Awarns -AInterningChecker_skipDefs=NullnessInterning
 *
 * @compile/ref=NullnessInterning3.out -XDrawDiagnostics -Anomsgtext -processor checkers.nullness.NullnessChecker,checkers.interning.InterningChecker NullnessInterning.java -Awarns -Acheckers.interning.InterningChecker_skipDefs=NullnessInterning
 *
 * @compile/ref=NullnessInterning4.out -XDrawDiagnostics -Anomsgtext -processor checkers.nullness.NullnessChecker,checkers.interning.InterningChecker NullnessInterning.java -Awarns -Acheckers.basetype.BaseTypeChecker_skipDefs=NullnessInterning
 */

class NullnessInterning {
    Object f = null;

    void m(Object p) {
        if (f == p) {
        }
    }
}
