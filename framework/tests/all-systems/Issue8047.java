// A diamond that instantiates the enclosing class, appearing in a lambda body, crashed inference.
// The type parameters of the instantiated class are type parameters of the constructor (JLS
// 15.9.3), so theta maps `Adapter`'s `T` to an inference variable.  `T` is also in scope at the
// lambda body, so applying theta to the type of the lambda parameter `input` replaced `T` by that
// inference variable, turning the constraint `input -> T#0` into `T#0 -> T#0`.  `T#0` was then left
// with no lower bound and resolved to `Object`, and the resulting `Adapter<Object>` crashed
// StructuralEqualityComparer when it was compared against the target type's `Adapter<T>`.

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Issue8047 {

  static class Adapter<T> {
    Adapter(T t) {}

    List<Adapter<T>> viaStream(Stream<T> s) {
      return s.map(input -> new Adapter<>(input)).collect(Collectors.toList());
    }

    List<Adapter<T>> viaList(List<T> l) {
      return l.stream().map(input -> new Adapter<>(input)).collect(Collectors.toList());
    }

    // Spelling out the type argument instead of using a diamond did not crash.
    List<Adapter<T>> explicitTypeArgument(Stream<T> s) {
      return s.map(input -> new Adapter<T>(input)).collect(Collectors.toList());
    }
  }

  // The same shape, without any JDK stream classes.

  interface Str<E> {
    <R> Str<R> map(Function<? super E, ? extends R> f);

    <X> X collect(Coll<? super E, X> c);
  }

  interface Coll<E, X> {}

  static <Y> Coll<Y, List<Y>> toList() {
    throw new AssertionError();
  }

  static class Wrapper<T> {
    Wrapper(T t) {}

    List<Wrapper<T>> f(Str<T> s) {
      return s.map(input -> new Wrapper<>(input)).collect(toList());
    }
  }

  // A diamond for a class other than the enclosing one did not crash.
  static class Other<T> {}

  static class Holder<T> {
    List<Other<T>> f(Stream<T> s) {
      return s.map(input -> new Other<T>()).collect(Collectors.toList());
    }
  }
}
