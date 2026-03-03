import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public abstract class Issue6664<A, B> {

  public static <A, B> Collector<Issue6664<A, B>, ?, Issue6664<A, List<B>>> breakIt() {
    return Collectors.reducing(
        Eithers.right(new ArrayList<>()),
        either -> either.map(Arrays::asList),
        (either, eithers) -> either);
  }

  public <C> Issue6664<A, C> map(Function<B, C> function) {
    throw new RuntimeException();
  }

  public static final class Eithers {
    public static <A, B> Issue6664<A, B> left(A left) {
      throw new RuntimeException();
    }

    public static <A, B> Issue6664<A, B> right(B right) {
      throw new RuntimeException();
    }
  }
}
