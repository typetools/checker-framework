/*
 * @test
 * @summary Test the defaulting mechanism for binary files.
 *
 * @compile -XDrawDiagnostics -Xlint:unchecked BinaryDefaultTestBinary.java
 * @compile/ref=BinaryDefaultTest.out -XDrawDiagnostics -Xlint:unchecked -processor lubglb.LubGlbChecker BinaryDefaultTest.java -AconservativeUntyped
 */

import lubglb.quals.*;

class BinaryDefaultTest {
  void test1(@B BinaryDefaultTestInterface bar) {
    @D BinaryDefaultTestBinary foo = BinaryDefaultTestBinary.foo(bar);
  }
}
