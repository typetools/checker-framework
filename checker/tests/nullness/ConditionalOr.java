import org.checkerframework.checker.nullness.qual.*;

public class ConditionalOr {

  void test(@Nullable Object o) {
    if (o == null || o.toString() == "...") {
      // ...
    }
  }
}
