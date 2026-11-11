import java.util.Iterator;

// The iterator() method of an Iterable may return a type variable.
public class EnhancedForIteratorTypeVariable {
  abstract static class StringIterable<I extends Iterator<String>> implements Iterable<String> {
    @Override
    public abstract I iterator();
  }

  abstract static class IntegerIterable<I extends Iterator<Integer>> implements Iterable<Integer> {
    @Override
    public abstract I iterator();
  }

  <I extends Iterator<String>> void strings(StringIterable<I> iterable) {
    for (String s : iterable) {}
  }

  void stringsWildcard(StringIterable<?> iterable) {
    for (String s : iterable) {}
  }

  <I extends Iterator<Integer>> long integers(IntegerIterable<I> iterable) {
    long sum = 0;
    for (int x : iterable) {
      sum += x;
    }
    return sum;
  }
}
