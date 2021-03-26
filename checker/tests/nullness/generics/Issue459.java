import org.checkerframework.checker.nullness.qual.*;

public class Issue459 {
  public class Generic<K, V> {}

  interface Iface<K, V> {
    public <K1 extends K, V1 extends V> Generic<K, V> foo(Generic<? super K1, ? super V1> arg);

    public <K2 extends K, V2 extends V> Generic<K, V> foo2(
        Generic<? super K2, ? super V2> arg, K2 strArg);
  }

  void f(Iface<Object, Object> arg, @NonNull String nnString) {
    final Generic<String, Integer> obj = new Generic<>();
    arg.foo(obj);

    final Generic<@Nullable String, Integer> obj2 = new Generic<>();
    arg.foo2(obj2, nnString);
  }
}
