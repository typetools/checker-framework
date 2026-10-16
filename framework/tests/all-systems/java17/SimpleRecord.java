// @below-java17-jdk-skip-test
// @infer-jaifs-skip-test The AFU's JAIF reading/writing libraries don't support records.
import java.util.function.Predicate;

record SimpleRecord<T extends Comparable<T>>(T root) implements Predicate<T> {
  @Override
  public boolean test(T t) {
    return root().compareTo(t) < 0;
  }
}
