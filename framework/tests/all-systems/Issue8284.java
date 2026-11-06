// Test case for https://github.com/typetools/checker-framework/issues/8284

import java.util.List;
import org.checkerframework.checker.nullness.qual.NonNull;

public class Issue8284 {
  static class TypeDescriptor<T> {}

  static class Getter<K, V> {}

  static <T> List<Getter<@NonNull T, Object>> getGetters(TypeDescriptor<T> typeDescriptor) {
    throw new RuntimeException();
  }

  static <T> List<Getter<@NonNull T, Object>> caller(TypeDescriptor<T> targetTypeDescriptor) {
    return getGetters(targetTypeDescriptor);
  }

  static <T> List<Getter<@NonNull T, Object>> explicitTypeArgument(TypeDescriptor<T> td) {
    return Issue8284.<T>getGetters(td);
  }

  static <T> List<Getter<T, Object>> plainGetters(TypeDescriptor<T> td) {
    throw new RuntimeException();
  }

  static <T> List<Getter<T, Object>> noAnnotation(TypeDescriptor<T> td) {
    return plainGetters(td);
  }

  static <T> void noTargetType(TypeDescriptor<T> td) {
    getGetters(td);
  }

  interface Consumer<T> {
    void apply(@NonNull T t);
  }

  static <X> X takeComparableConsumer(Consumer<? extends Comparable<X>> f, X x) {
    return x;
  }

  // The lambda's parameter type gives the bound T = @NonNull U, whose annotations are ignored.  It
  // still determines the Java type of the lambda's ground target type, Consumer<U>.
  static <U extends Comparable<U>> void explicitlyTypedLambda(U u0) {
    takeComparableConsumer((U u) -> {}, u0);
  }
}
