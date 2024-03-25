import java.util.List;


@SuppressWarnings("all") // Just check for crashes.
public class Issue6421 {

  public static final List<MatcherOperator<ArbitraryIntrospector>> DEFAULT_ARBITRARY_INTROSPECTORS =
      list(
          MatcherOperator.exactTypeMatchOperator(
              UnidentifiableType.class, NullArbitraryIntrospector.INSTANCE),
          MatcherOperator.exactTypeMatchOperator(
              GeneratingWildcardType.class, context -> new ArbitraryIntrospectorResult()));

  static <E> List<E> list(E e1, E e2) {
    throw new RuntimeException();
  }

  public interface ArbitraryIntrospector {
    ArbitraryIntrospectorResult introspect(ArbitraryGeneratorContext context);
  }

  public interface CombinableArbitrary<T> {}

  public static final class ArbitraryGeneratorContext {}

  public static final class MatcherOperator<T> {
    public static <T, C> MatcherOperator<T> exactTypeMatchOperator(Class<C> type, T operator) {
      throw new RuntimeException();
    }
  }

  public static class GeneratingWildcardType {}

  public static class UnidentifiableType {}

  public static final class NullArbitraryIntrospector implements ArbitraryIntrospector {

    public static final NullArbitraryIntrospector INSTANCE = new NullArbitraryIntrospector();

    @Override
    public ArbitraryIntrospectorResult introspect(ArbitraryGeneratorContext context) {
      return null;
    }
  }

  public static final class ArbitraryIntrospectorResult {
    public ArbitraryIntrospectorResult() {}

    public ArbitraryIntrospectorResult(CombinableArbitrary<?> value) {}
  }
}
