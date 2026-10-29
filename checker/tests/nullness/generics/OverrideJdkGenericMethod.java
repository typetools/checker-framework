// Overriding a generic JDK method requires no annotation on the overriding method's type
// parameter, even though the Map Key Checker's default for lower bounds is @KeyForBottom in the
// JDK and @UnknownKeyFor elsewhere.

import java.util.AbstractCollection;
import java.util.Comparator;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.function.IntFunction;

public class OverrideJdkGenericMethod {

  abstract static class MyExecutorService extends AbstractExecutorService {
    @Override
    public <T> Future<T> submit(Callable<T> task) {
      throw new Error("not implemented");
    }
  }

  abstract static class MyCollection<E> extends AbstractCollection<E> {
    @Override
    public <T> T[] toArray(IntFunction<T[]> generator) {
      throw new Error("not implemented");
    }
  }

  abstract static class MyComparator<T> implements Comparator<T> {
    @Override
    public <U extends Comparable<? super U>> Comparator<T> thenComparing(
        Function<? super T, ? extends U> keyExtractor) {
      throw new Error("not implemented");
    }
  }
}
