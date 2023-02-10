/*
 * @test
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
