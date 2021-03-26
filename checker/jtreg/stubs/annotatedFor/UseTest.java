/*
 * @test
 * @summary Test AnnotatedFor in stub files
 * @compile -XDrawDiagnostics -Xlint:unchecked ../annotatedForLib/Test.java
 * @compile/fail/ref=WithStub.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext UseTest.java -Astubs=Test.astub -AstubWarnIfNotFound -Werror
 * @compile/fail/ref=WithoutStub.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext UseTest.java -AstubWarnIfNotFound -Werror
 */

package annotatedfor;

import annotatedforlib.Test;

public class UseTest {
  void test(Test<String> test) {
    test.method1(null);
    test.method2(null);
  }
}
