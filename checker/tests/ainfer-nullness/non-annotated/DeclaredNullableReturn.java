// @Nullable is the top qualifier, but it is not the default for a return type, so whole-program
// inference must write the @Nullable that it infers for this return type.

import org.checkerframework.checker.nullness.qual.Nullable;

public class DeclaredNullableReturn {
  public String identity(@Nullable String s) {
    // :: warning: [return]
    return s;
  }
}
