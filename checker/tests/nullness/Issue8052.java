// Test case for Issue 8052:
// https://github.com/typetools/checker-framework/issues/8052

// The all-systems test with the same name checks that these shapes do not crash inference.  This
// test checks the inference *result*: a `return` statement in a member of a class declared in a
// lambda body is not a result expression of the lambda (JLS 14.17), so it must not contribute to
// the lambda's inferred result type.

import java.util.List;
import java.util.stream.Collectors;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class Issue8052 {

  interface NullableSupplier {
    @Nullable String get();
  }

  // The only result expression of the lambda is "x", which is @NonNull, so R infers to
  // @NonNull String.  Before the fix, the `return null` in the anonymous class was collected as a
  // result expression too, R inferred to @Nullable String, and the collect(...) call reported
  // type.arguments.not.inferred.
  List<@NonNull String> anonymousClass(List<String> l) {
    return l.stream()
        .map(
            (v) -> {
              NullableSupplier unrelated =
                  new NullableSupplier() {
                    @Override
                    public @Nullable String get() {
                      return null;
                    }
                  };
              return "x";
            })
        .collect(Collectors.toList());
  }

  // Same, for a local class.
  List<@NonNull String> localClass(List<String> l) {
    return l.stream()
        .map(
            (v) -> {
              class Local {
                @Nullable String s() {
                  return null;
                }
              }
              return "x";
            })
        .collect(Collectors.toList());
  }

  // The lambda's own result expressions are still collected: here one of them is @Nullable, so R
  // infers to @Nullable String and the assignment to List<@NonNull String> is an error.  This
  // guards against the cutoff being too aggressive.
  List<@NonNull String> ownReturnsStillCounted(List<String> l, boolean b) {
    return l.stream()
        .map(
            (v) -> {
              if (b) {
                return null;
              }
              return "x";
            })
        // :: error: (type.arguments.not.inferred)
        .collect(Collectors.toList());
  }

  // A return in a class declared inside a *nested* lambda is likewise not a result expression of
  // the outer lambda.
  List<@NonNull String> classInsideNestedLambda(List<String> l) {
    return l.stream()
        .map(
            (v) -> {
              NullableSupplier unrelated =
                  () -> {
                    NullableSupplier inner =
                        new NullableSupplier() {
                          @Override
                          public @Nullable String get() {
                            return null;
                          }
                        };
                    return null;
                  };
              return "x";
            })
        .collect(Collectors.toList());
  }
}
