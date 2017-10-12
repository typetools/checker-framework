import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

// Testcase for Issue #679
// https://github.com/typetools/checker-framework/issues/679
// @skip-test
public class Issue679 {
    interface Interface<T> {}

    class B implements Interface<@NonNull Number> {}

    // :: error: Interface cannot be inherited with different arguments: <@NonNull Number> and
    // <@Nullable Number>
    class A extends B implements Interface<@Nullable Number> {}
}
