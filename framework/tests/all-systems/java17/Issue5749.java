// @below-java17-jdk-skip-test
import java.util.stream.Stream;

public record Issue5749() {

  public enum Bar {
    A,
    B,
    C
  }

  public static final Stream<String> BAR = Stream.of(Bar.values()).map(b -> "");
}
