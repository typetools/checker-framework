// @below-java17-jdk-skip-test
import java.util.List;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class SwitchExpressionInvariant {
  public static boolean flag = false;

  void method(
      List<@NonNull String> nonnullStrings, List<@Nullable String> nullableStrings, int fenum) {
    List<@NonNull String> list =
        switch (fenum) {
          case 1 -> nonnullStrings;
          default -> nullableStrings;
        };

    List<@Nullable String> list2 =
        switch (fenum) {
          case 1 -> nonnullStrings;
          default -> nullableStrings;
        };
  }
}
