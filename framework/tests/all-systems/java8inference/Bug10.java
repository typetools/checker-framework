package inference.guava;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collector;

@SuppressWarnings("all") // Just check for crashes.
public class Bug10 {
  public static <T> Collector<T, ?, List<T>> least(int k, Comparator<? super T> comparator) {
    return Collector.of(
        () -> TopKSelector.<T>least(k, comparator),
        TopKSelector::offer,
        TopKSelector::combine,
        TopKSelector::topK,
        Collector.Characteristics.UNORDERED);
  }

  static final class TopKSelector<T> {
    TopKSelector<T> combine(TopKSelector<T> other) {
      throw new RuntimeException();
    }

    public static <T extends Comparable<? super T>> TopKSelector<T> least(int k) {
      throw new RuntimeException();
    }

    public static <T extends Comparable<? super T>> TopKSelector<T> greatest(int k) {
      throw new RuntimeException();
    }

    public static <T> TopKSelector<T> least(int k, Comparator<? super T> comparator) {
      return new TopKSelector<T>(comparator, k);
    }

    public static <T> TopKSelector<T> greatest(int k, Comparator<? super T> comparator) {
      throw new RuntimeException();
    }

    private TopKSelector(Comparator<? super T> comparator, int k) {}

    public void offer(T elem) {
      throw new RuntimeException();
    }

    public List<T> topK() {
      throw new RuntimeException();
    }
  }
}
