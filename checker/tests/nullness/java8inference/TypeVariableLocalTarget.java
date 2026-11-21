// Test case for type argument inference whose target type is a local variable whose type is a type
// variable.  The target type that inference uses lacks the local variable default, @Nullable (see
// InferenceFactory#assignedToVariable), so inference with the target type fails.  But the check of
// the initialization uses the default, so the initialization is legal and no error is expected.

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class TypeVariableLocalTarget<T extends @Nullable Object> {

  static <U extends @Nullable Object> U id(U u) {
    return u;
  }

  T field;

  TypeVariableLocalTarget(T t) {
    field = t;
  }

  void local(@Nullable T nullable) {
    // Inference with the target type fails on both of these lines.  If inference were not retried
    // without the target type, then both would report a spurious "type.arguments.not.inferred".
    T t = id(nullable);
    T t2 = id(id(nullable));
    // :: error: [assignment]
    field = t;
    @Nullable T t3 = t2;
  }

  void local2(T naked) {
    // Inference with the target type succeeds on both of these lines, because the type of naked is
    // the target type.  So inference is not retried, and no "type.arguments.not.inferred" is
    // reported, regardless of the retry.
    T t = id(naked);
    T t2 = id(id(naked));
    field = t;
    @Nullable T t3 = t2;
    // :: error: [assignment]
    @NonNull T t4 = t2;
  }

  void nonNullLocal(@Nullable T nullable) {
    // :: error: [assignment]
    @NonNull T t = id(nullable);
  }

  void fieldAssignment(@Nullable T nullable) {
    // The local variable default does not apply to fields.
    // :: error: [assignment]
    field = id(nullable);
  }

  T returnStatement(@Nullable T nullable) {
    // :: error: [return]
    return id(nullable);
  }
}
