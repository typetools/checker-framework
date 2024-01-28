import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

//  @below-java17-jdk-skip-test
public class SwitchExpressionTypeArgInference {
  <T> T method(T t) {
    return t;
  }

  void test1(int i, @Nullable String nullable) {
    @NonNull String s =
        // :: error: (assignment)
        // :: error: (type.arguments.not.inferred)
        method(
            switch (i) {
              case 0:
                yield method(nullable);
              case 1:
                yield "";
              default:
                yield "";
            });
  }

  void test2(int i, @Nullable String nullable) {
    @NonNull String s =
        method(
            switch (i) {
              case 0:
                yield method("nullable");
              case 1:
                yield "";
              default:
                yield "";
            });
  }
}
