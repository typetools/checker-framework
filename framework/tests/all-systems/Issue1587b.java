abstract class Issue1587b {

  static class One implements Two<One, Three, Four, Four> {}

  interface Two<
      E extends Two<E, K, C, I>, K extends Five<K>, C extends Enum<C>, I extends Enum<I>> {}

  abstract static class Three implements Five<Three> {}

  enum Four {}

  interface Five<K extends Five<K>> extends Comparable<K> {}

  interface Six {
    <E extends Two<E, ?, ?, I>, I extends Enum<I>> Seven<?, E> e(Class<E> entity);
  }

  interface Seven<T extends Seven<T, E>, E extends Two<E, ?, ?, ?>> extends Eight<T, E, E> {}

  interface Eight<T extends Eight<T, R, E>, R, E extends Two<E, ?, ?, ?>> extends Nine<T, R> {}

  interface Nine<T extends Nine<T, R>, R> {
    T d();

    Iterable<R> q();
  }

  public Iterable<One> f(Six e) {
    return g(e.e(One.class).d().q());
  }

  abstract Iterable<One> g(Iterable<One> r);
}
