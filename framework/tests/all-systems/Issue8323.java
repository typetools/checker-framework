import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Issue8323 {
  interface FB<K, V extends Comparable<V>> {}

  interface Two<K, V extends Number & Comparable<V>> {}

  interface Box<T> {
    <R> Box<R> map(Function<? super T, ? extends R> f);

    <S> S get(Getter<? super T, S> g);
  }

  interface Getter<T, S> {}

  static native <T> Getter<T, List<T>> toList();

  static native <P> FB<P, ? extends Integer> fbExtends(P p);

  static native <P> FB<P, ?> fbUnbounded(P p);

  static native <P> Two<P, ? extends Integer> twoExtends(P p);

  static native <P> Two<P, ?> twoUnbounded(P p);

  static List<FB<?, ?>> m(Box<String> b) {
    return b.map(t -> fbExtends(t)).get(toList());
  }

  static List<FB<?, ?>> unbounded(Box<String> b) {
    return b.map(t -> fbUnbounded(t)).get(toList());
  }

  static List<FB<?, ?>> methodRef(Box<String> b) {
    return b.map(Issue8323::fbExtends).get(toList());
  }

  static List<FB<?, ?>> stream(List<String> l) {
    return l.stream().map(t -> fbExtends(t)).collect(Collectors.toList());
  }

  static List<FB<?, ? extends Integer>> streamExtendsTarget(List<String> l) {
    return l.stream().map(t -> fbExtends(t)).collect(Collectors.toList());
  }

  static List<FB<?, ?>> streamUnbounded(List<String> l) {
    return l.stream().map(t -> fbUnbounded(t)).collect(Collectors.toList());
  }

  static List<Two<?, ?>> streamTwo(List<String> l) {
    return l.stream().map(t -> twoExtends(t)).collect(Collectors.toList());
  }

  static List<Two<?, ?>> streamTwoUnbounded(List<String> l) {
    return l.stream().map(t -> twoUnbounded(t)).collect(Collectors.toList());
  }
}
