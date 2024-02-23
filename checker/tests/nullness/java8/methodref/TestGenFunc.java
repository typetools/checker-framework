import org.checkerframework.checker.nullness.qual.*;

public class TestGenFunc {
  interface FuncNullableParam {
    <T extends @Nullable Number, U extends @Nullable Number> T nullableParam(U u);
  }

  interface FuncNonNullParam {
    <T extends @Nullable Number, U extends @NonNull Number> T nonNullParam(U u);
  }

  static <V extends @NonNull Number, P extends @Nullable Number> V nonNullReturn(P u) {
    throw new RuntimeException("");
  }

  static <V extends @Nullable Number, P extends @NonNull Number> V nonNullParameter(P u) {
    throw new RuntimeException("");
  }

  static <V extends @Nullable Number, P extends @Nullable Number> V allNullable(P u) {
    throw new RuntimeException("");
  }

  void context() {
    // :: error: (type.arguments.not.inferred)
    FuncNullableParam f = TestGenFunc::nonNullReturn;
    // :: error: (type.arguments.not.inferred)
    FuncNonNullParam f2 = TestGenFunc::nonNullReturn;
  }

  void context2() {
    // :: error: (type.arguments.not.inferred)
    FuncNullableParam f = TestGenFunc::nonNullParameter;
    FuncNonNullParam f2 = TestGenFunc::nonNullParameter;
  }

  void context3() {
    FuncNullableParam f = TestGenFunc::allNullable;
    FuncNonNullParam f2 = TestGenFunc::allNullable;
  }
}
