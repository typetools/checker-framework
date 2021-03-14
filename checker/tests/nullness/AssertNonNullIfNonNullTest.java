import org.checkerframework.checker.nullness.qual.AssertNonNullIfNonNull;
import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.Pure;

// @skip-test Re-enable when @AssertNonNullIfNonNull checking is enhanced

public class AssertNonNullIfNonNullTest {

  private @Nullable String value;

  @Pure
  @AssertNonNullIfNonNull("value")
  public @Nullable String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  @EnsuresNonNullIf(expression = "value", result = true)
  public boolean isValueNonNull1() {
    return value != null;
  }

  @EnsuresNonNullIf(expression = "getValue()", result = true)
  public boolean isValueNonNull2() {
    // The @AssertNonNullIfNonNull annotation implies that if getValue() is
    // non-null, then is non-null, then value is non-null, but not the
    // converse, so an error should be issued here.
    // :: error: (contracts.conditional.postcondition.not.satisfied)
    return value != null;
  }

  // The @AssertNonNullIfNonNull annotation should enable suppressing this error.
  @EnsuresNonNullIf(expression = "value", result = true)
  public boolean isValueNonNull3() {
    return getValue() != null;
  }

  @EnsuresNonNullIf(expression = "getValue()", result = true)
  public boolean isValueNonNull4() {
    return getValue() != null;
  }
}
