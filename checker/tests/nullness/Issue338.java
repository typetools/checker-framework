interface Foo338<T> {
  Class<T> get();
}

public class Issue338 {
  static void m2(Foo338<?> foo) {
    Class<?> clazz = foo.get();
  }
}
