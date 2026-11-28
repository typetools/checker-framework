import org.checkerframework.checker.nullness.qual.Nullable;

// Whole-program inference infers `@EnsuresNonNull("this.field")` for `setField`.  `@NonNull`, the
// qualifier in that postcondition, is the default qualifier, but the postcondition is not
// redundant, because the declared type of the field is `@Nullable`.
public class EnsuresNonNullInference {

  @Nullable String field;

  void setField() {
    field = "hello";
  }

  void client() {
    setField();
    // :: warning: [dereference.of.nullable]
    field.toString();
  }
}
