/*
 * @test
 * @summary Test the defaulting mechanism for binary files.
 *
 * @compile -XDrawDiagnostics -Xlint:unchecked BinaryDefaultTestBinary.java
 * @compile -XDrawDiagnostics -Xlint:unchecked -processor lubglb.LubGlbChecker BinaryDefaultTest.java -AuseDefaultsForUncheckedCode=-source,bytecode -AprintErrorStack
 */

import lubglb.quals.*;

class BinaryDefaultTest {
  void test1(@B BinaryDefaultTestInterface bar) {
    @F BinaryDefaultTestBinary foo = BinaryDefaultTestBinary.foo(bar);
  }
}
