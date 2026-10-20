import java.util.function.Supplier;
import org.checkerframework.checker.nullness.qual.NonNull;

// @below-java17-jdk-skip-test
public final class Issue5967 {

  enum TestEnum {
    FIRST,
    SECOND;
  }

  public static void main(String[] args) {
    TestEnum testEnum = TestEnum.FIRST;
    Supplier<Integer> supplier =
        switch (testEnum) {
          case FIRST:
            yield () -> 1;
          case SECOND:
            yield () -> 2;
        };
    @NonNull Supplier<Integer> supplier1 = supplier;
  }
}
