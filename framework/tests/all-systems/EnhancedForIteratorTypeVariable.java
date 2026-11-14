import java.util.Iterator;
import java.util.List;

// The iterator() method of an Iterable may return a type variable.
public class EnhancedForIteratorTypeVariable {
  abstract static class StringIterable<I extends Iterator<String>> implements Iterable<String> {
    @SuppressWarnings({
      "unneeded.suppression", // nonempty only issues an error when run alone.
      "nonempty:override.return" // Iterable.iterator() returns @PolyNonEmpty.
    })
    @Override
    public abstract I iterator();
  }

  abstract static class IntegerIterable<I extends Iterator<Integer>> implements Iterable<Integer> {
    @SuppressWarnings({
      "unneeded.suppression", // nonempty only issues an error when run alone.
      "nonempty:override.return" // Iterable.iterator() returns @PolyNonEmpty.
    })
    @Override
    public abstract I iterator();
  }

  <I extends Iterator<String>> void strings(StringIterable<I> iterable) {
    for (String s : iterable) {}
  }

  void stringsWildcard(StringIterable<?> iterable) {
    for (String s : iterable) {}
  }

  // The upper bound of `T` is the uncaptured type `StringIterable<?>`.
  <T extends StringIterable<?>> void stringsTypeVariableWithWildcard(T iterable) {
    for (String s : iterable) {}
  }

  <I extends Iterator<Integer>> long integers(IntegerIterable<I> iterable) {
    long sum = 0;
    for (int x : iterable) {
      sum += x;
    }
    for (long x : iterable) {
      sum += x;
    }
    return sum;
  }

  // The element type is `Integer`, the type argument of `List<Integer>`, so the loop unboxes and
  // widens each element.
  long integerList(List<Integer> list) {
    long sum = 0;
    for (long x : list) {
      sum += x;
    }
    return sum;
  }

  // The element type is the capture of `? extends Integer`.
  long integerWildcardList(List<? extends Integer> list) {
    long sum = 0;
    for (int x : list) {
      sum += x;
    }
    for (long x : list) {
      sum += x;
    }
    return sum;
  }
}
