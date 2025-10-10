package open;

import java.util.function.Function;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class LambdaFlow {

  void flowIntoLambda(@Nullable String param) {
    if (param != null) {
      Function<String, @NonNull String> b = s -> param;
    }
  }

  void flowOutOfLambda(@Nullable String param) {
    Function<String, @NonNull String> d =
        s -> {
          if (param != null) {
            return param;
          } else {
            return "";
          }
        };
  }

  void flowOutOfLambda2(@Nullable String param) {
    @NonNull String f =
        foo(
            s -> {
              if (param != null) {
                return param;
              } else {
                return "";
              }
            });
  }

  void flowIntoThenOutOfLambda(@Nullable String param) {
    if (param != null) {
      @NonNull String h = foo(s -> param);
    }
  }

  // Same examples above but with a field instead of a parameter.

  @Nullable String field;

  void flowIntoLambdaField() {
    if (field != null) {
      Function<String, @NonNull String> a =
          // :: error: (return)
          s -> field;
    }
  }

  void flowOutOfLambda() {
    Function<String, @NonNull String> c =
        s -> {
          if (field != null) {
            return field;
          } else {
            return "";
          }
        };
  }

  void flowOutOfLambda2() {
    @NonNull String e =
        foo(
            s -> {
              if (field != null) {
                return field;
              } else {
                return "";
              }
            });
  }

  void flowIntoThenOutOfLambda() {
    if (field != null) {
      // :: error: (type.arguments.not.inferred) :: error: (assignment)
      @NonNull String g = foo(s -> field);
    }
  }

  <T> T foo(Function<String, T> func) {
    return func.apply("");
  }
}
