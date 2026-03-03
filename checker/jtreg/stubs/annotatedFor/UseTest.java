/*
 * @test
 * @summary Test AnnotatedFor in stub files
 * @compile -Xlint:unchecked ../annotatedForLib/Test.java
 * @compile/fail/ref=WithStub.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext UseTest.java -Astubs=Test.astub -Werror
 * @compile/fail/ref=WithStub.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext UseTest.java -Astubs=Test.astub -Werror -AuseConservativeDefaultsForUncheckedCode=bytecode
 * @compile/fail/ref=WithoutStub.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext UseTest.java -Werror
 * @compile/fail/ref=WithoutStubConservative.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext UseTest.java -Werror -AuseConservativeDefaultsForUncheckedCode=bytecode
 */

package annotatedfor;

import annotatedforlib.Test;
import org.checkerframework.checker.nullness.qual.NonNull;

public class UseTest {
  void test(Test<String> test) {
    test.method1(null);
    test.method2(null);
    @NonNull Object o = test.method3();
  }
}
