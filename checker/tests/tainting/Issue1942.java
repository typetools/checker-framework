import java.util.List;
import org.checkerframework.checker.tainting.qual.Untainted;

public class Issue1942 {
  public interface LoadableExpression<EXPRESSION> {}

  abstract static class OperatorSection<A extends LoadableExpression<A>> {
    abstract A makeExpression(List<@Untainted A> expressions);
  }

  static class BinaryOperatorSection<B extends LoadableExpression<B>> extends OperatorSection<B> {
    @Override
    // Override used to fail.
    B makeExpression(List<@Untainted B> expressions) {
      throw new RuntimeException("");
    }
  }
}
