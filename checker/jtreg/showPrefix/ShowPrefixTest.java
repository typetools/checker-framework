/*
 * @test
 * @summary Test that Issue 469 is fixed. Thanks to user pSub for the test case.
 * @library .
 *
 * @compile/fail/ref=ShowPrefixTest.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker ShowPrefixTest.java -AshowPrefixInWarningMessages
 */

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class ShowPrefixTest {
  @NonNull Object foo(@Nullable Object nble) {
    return nble;
  }
}
