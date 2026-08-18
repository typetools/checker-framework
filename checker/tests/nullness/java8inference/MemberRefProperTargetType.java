// Tests type argument inference for a generic method reference whose target type is already a
// proper type by the time its constraint formula is reduced.
//
// When a method reference that elides type arguments is an argument to a generic method, the
// method reference's own type arguments are usually inferred as part of the outer invocation's
// inference problem.  That does not happen when the target type's function type has a parameter
// type mentioning one of the outer invocation's inference variables: JLS 18.5.2.2 makes such a
// variable an input variable of the <MethodReference -> T> constraint, so it is resolved and
// substituted into the constraint before the constraint is reduced.  T is then a proper type, and
// JLS 18.2.1's rule for <MethodReference -> T> -- which applies only where "T mentions at least
// one inference variable" -- does not apply.  The outer inference therefore creates no inference
// variables for the method reference, and its type arguments must be inferred separately, against
// the now-proper target type.
//
// In each call below, the outer method's type variable T appears in the function type's parameter
// type, so T is resolved before the method reference is reduced.  If the method reference's type
// arguments are not inferred afterwards, its type parameters stay uninstantiated and the calls in
// noError() are reported as methodref.param errors.

import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class MemberRefProperTargetType {

  // The function type of Consumer<T> is "void accept(T)": its result is void.
  static <T> void consume(Consumer<T> c) {}

  static <T> void consumeNullable(Consumer<@Nullable T> c) {}

  // The function type of UnaryOperator<T> is "T apply(T)": its result is not void.
  static <T> void applyUnary(UnaryOperator<T> op) {}

  static <T> void applyUnaryNonNull(UnaryOperator<@NonNull T> op) {}

  static <T> void sink(T t) {}

  static <T> void sinkNonNull(@NonNull T t) {}

  static <T> T identity(T t) {
    return t;
  }

  static <T> @Nullable T toNullable(T t) {
    return null;
  }

  void noError() {
    consume(MemberRefProperTargetType::sink);
    applyUnary(MemberRefProperTargetType::identity);
  }

  void parameterMismatch() {
    // The function type's parameter is @Nullable T, but sinkNonNull requires @NonNull T.
    // :: error: (methodref.param)
    consumeNullable(MemberRefProperTargetType::sinkNonNull);
  }

  void returnMismatch() {
    // The function type's result is @NonNull T, but toNullable returns @Nullable T.
    // :: error: (methodref.return)
    applyUnaryNonNull(MemberRefProperTargetType::toNullable);
  }
}
