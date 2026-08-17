package open.liam;

// Bug in org.checkerframework.framework.type.TypeFromMemberVisitor.inferLambdaParamAnnotations.
@SuppressWarnings("all") // Checker for classes.
public class Issue7677 {
  static class Box<T> {
    static <T> Box<T> of(T t) {
      return new Box<>();
    }

    <R> Box<R> map(MyFunction<T, R> f) {
      return new Box<>();
    }
  }

  interface MyConsumer<V> {
    void accept(V v);
  }

  interface MyFunction<A, B> {
    B apply(A a);
  }

  <V> void use(V v, MyConsumer<V> consumer) {}

  void test(Box<Long> box) {
    use(box, m -> m.map(e -> Box.of(getBoolean() ? e : 0L)));
  }

  Boolean getBoolean() {
    return true;
  }
}
