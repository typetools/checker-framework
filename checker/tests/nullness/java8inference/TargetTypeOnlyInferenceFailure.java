// Test case for type argument inference that fails only because of the target type.  Such an
// invocation is reported once, by the check of the assignment, return, or argument that gives the
// invocation its target type, rather than also by "type.arguments.not.inferred".

import java.util.List;
import java.util.function.Function;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class TargetTypeOnlyInferenceFailure {

  static <T extends @Nullable Object> List<T> singleton(T t) {
    throw new Error();
  }

  static <T extends @Nullable Object> List<T> singleton2(T t, Object o) {
    throw new Error();
  }

  static <T extends @Nullable Object> List<? extends T> wildcardSingleton(T t) {
    throw new Error();
  }

  static <T extends @Nullable Object> T id(T t) {
    return t;
  }

  static <T extends @Nullable Object, U extends T> U sub(List<T> a, U u) {
    throw new Error();
  }

  static <T extends @Nullable Object, U extends T> U subWithObject(List<T> a, U u, Object o) {
    throw new Error();
  }

  static <T extends @Nullable Object> T apply(Function<String, T> f) {
    throw new Error();
  }

  void takesNonNullList(List<@NonNull String> l) {}

  void assignment(@Nullable String s) {
    // :: error: [assignment]
    List<@NonNull String> l = singleton(s);
  }

  List<@NonNull String> returnStatement(@Nullable String s) {
    // :: error: [return]
    return singleton(s);
  }

  void argument(@Nullable String s) {
    // :: error: [argument]
    takesNonNullList(singleton(s));
  }

  void wildcardReturnType(@Nullable String s) {
    // The return type is captured, which creates inference variables for the capture.
    // :: error: [assignment]
    List<? extends @NonNull String> l = wildcardSingleton(s);
  }

  void nestedInvocation(@Nullable String s) {
    // The nested invocations and the outer invocation are inferred together; only the target type
    // of the outermost invocation is incompatible.
    // :: error: [assignment]
    List<@NonNull String> l = id(id(singleton(s)));
    // :: error: [argument]
    takesNonNullList(id(singleton(s)));
  }

  void compoundAssignment(@NonNull String str, @Nullable String s, Integer i, @Nullable Integer n) {
    // The target type of id(s) is the type of str, which id(s) is not compatible with.  But the
    // compound assignment is legal, because string concatenation permits a null operand.
    str += id(s);
    // :: error: [unboxing.of.nullable]
    i += id(n);
  }

  void otherChecksOfTheInvocation(@Nullable String s, @Nullable Object o) {
    // The argument check of the invocation is performed too.
    // :: error: [assignment] :: error: [argument]
    List<@NonNull String> l = singleton2(s, o);
  }

  void independentOfTarget(List<@NonNull String> a, @Nullable String n) {
    // Inference fails because of the arguments, regardless of the target type.
    // :: error: [type.arguments.not.inferred]
    sub(a, n);
    // :: error: [type.arguments.not.inferred]
    @Nullable String s = sub(a, n);
  }

  void errorInArgumentOfFailedInference(
      List<@NonNull String> a, @Nullable String n, @Nullable Object o) {
    // The arguments of an invocation whose inference fails are checked too.
    // :: error: [type.arguments.not.inferred] :: error: [dereference.of.nullable]
    sub(a, o.hashCode() > 0 ? n : null);
    // The failure of an inference problem is reported once, even though it has two invocations.
    // :: error: [type.arguments.not.inferred]
    id(sub(a, n));
  }

  void independentArgumentOfFailedInference(
      List<@NonNull String> a, @Nullable String n, @Nullable Object o) {
    // An argument whose parameter type does not mention an inferred type variable is checked.
    // :: error: [type.arguments.not.inferred] :: error: [argument]
    subWithObject(a, n, o);
  }

  void javaTypeFromTarget() {
    // Without the target type, the Java type argument would be Object rather than String, so the
    // target type is needed, and inference fails.  The body of the lambda is checked against the
    // function type that inference instantiated.
    // :: error: [type.arguments.not.inferred] :: error: [return]
    @NonNull String r = apply(x -> null);
  }
}
