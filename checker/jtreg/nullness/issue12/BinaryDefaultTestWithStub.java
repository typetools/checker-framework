/*
 * @test
 * @summary Test the defaulting mechanism for nullness in binary files, but with stub files.
 * Because source defaults are used in stub files, only the error for the invalid
 * argument is expected.
 *
 * @ignore Temporarily, until safe defaults fon unannotated libraries are the default
 * @compile -XDrawDiagnostics -Xlint:unchecked BinaryDefaultTestBinary.java
 * @compile/fail/ref=BinaryDefaultTestWithStub.out -XDrawDiagnostics -Xlint:unchecked -Astubs=binary.astub -processor org.checkerframework.checker.nullness.NullnessChecker BinaryDefaultTest.java
 */

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class BinaryDefaultTestWithStub {
    void test1(@NonNull BinaryDefaultTestInterface bar, @Nullable BinaryDefaultTestInterface bar2) {
        @Nullable BinaryDefaultTestBinary foo = BinaryDefaultTestBinary.foo(bar);
        @Nullable BinaryDefaultTestBinary baz = BinaryDefaultTestBinary.foo(bar2);
        @NonNull BinaryDefaultTestBinary biz = BinaryDefaultTestBinary.foo(bar);
    }
}
