import java.io.Serializable;

// @infer-ajava-skip-test
@SuppressWarnings("all") // Just check for crashes.
public class GuavaCrash<C extends Comparable<?>> {
  void method(Range<C> restriction, Range<Cut<C>> lowerBoundWindow) {
    Cut<Cut<C>> upperBoundOnLowerBounds =
        Ordering.natural().min(lowerBoundWindow.upperBound, Cut.belowValue(restriction.upperBound));
  }

  abstract static class Cut<C extends Comparable> implements Comparable<Cut<C>>, Serializable {
    static <C extends Comparable> Cut<C> belowValue(C endpoint) {
      throw new RuntimeException();
    }
  }

  static class Ordering<T> {
    public static <C extends Comparable> Ordering<C> natural() {
      throw new RuntimeException();
    }

    public <E extends T> E min(E a, E b) {
      throw new RuntimeException();
    }
  }

  public static final class Range<C extends Comparable> {
    final Cut<C> upperBound =
        new Cut<C>() {
          @Override
          public int compareTo(Cut<C> o) {
            return 0;
          }
        };
  }
}
