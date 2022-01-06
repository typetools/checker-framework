// @below-java17-jdk-skip-test
import java.util.function.Function;
import org.checkerframework.checker.nullness.qual.Nullable;

public class NullnessSwitchExpressionLambda {
  int anInt;

  void switchExprLambda() {
    Function<NullnessSwitchExpressionLambda, @Nullable Object> f =
        (n) ->
            switch (n.anInt) {
              case 3, 4, 5 -> new Object();
              default -> null;
            };
    Object o = f.apply(new NullnessSwitchExpressionLambda());
    // :: error: (dereference.of.nullable)
    o.toString();
  }
}
