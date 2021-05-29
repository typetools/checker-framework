// Test case for #953
// https://github.com/typetools/checker-framework/issues/953

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.checkerframework.checker.nullness.qual.NonNull;

@SuppressWarnings("all")
public class Issue953bLambda {
  private static List<List<?>> strs = new ArrayList<>();

  public static <R, T> List<@NonNull R> mapList(
      List<@NonNull T> list, Function<@NonNull T, @NonNull R> func) {
    throw new RuntimeException();
  }

  public static void test() {
    List<String> list =
        mapList(
            strs,
            s -> {
              return "";
            });
  }
}
