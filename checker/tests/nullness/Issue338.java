interface Foo338<T> {
    Class<T> get();
}

class Issue338 {
    static void m2(Foo338<?> foo) {
        Class<?> clazz = foo.get();
    }
}
