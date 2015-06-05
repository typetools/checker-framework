/*
 * @test
 * @summary Test the defaulting mechanism for nullness in binary files.
 *
 * @ignore
 * @compile -XDrawDiagnostics -Xlint:unchecked BinaryDefaultTestBinary.java
 * @compile/fail/ref=BinaryDefaultTest.out -XDrawDiagnostics -Xlint:unchecked -processor org.checkerframework.checker.nullness.NullnessChecker BinaryDefaultTest.java
 */

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.NonNull;

class BinaryDefaultTest {
  void test1(@NonNull BinaryDefaultTestInterface bar, @Nullable BinaryDefaultTestInterface bar2) {
    @Nullable BinaryDefaultTestBinary foo = BinaryDefaultTestBinary.foo(bar);
    @Nullable BinaryDefaultTestBinary baz = BinaryDefaultTestBinary.foo(bar2);
    @NonNull BinaryDefaultTestBinary biz = BinaryDefaultTestBinary.foo(bar);
  }
}
