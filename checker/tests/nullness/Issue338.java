interface Foo<T> {
  Class<T> get();
}

class Issue338 {
  static void m2(Foo<?> foo) {
    Class<?> clazz = foo.get();
  }
}