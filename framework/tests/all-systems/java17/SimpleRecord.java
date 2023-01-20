// @below-java17-jdk-skip-test
import java.util.function.Predicate;

record SimpleRecord<T extends Comparable<T>>(T root) implements Predicate<T> {
  @Override
  public boolean test(T t) {
    return root().compareTo(t) < 0;
  }
}
