class One {
  abstract static class B<A extends B<A, C>, C extends One> {
    abstract C build();

    A f() {
      throw new AssertionError();
    }
  }
}

class Two extends One {
  static class B<D extends B<D>> extends One.B<D, Two> {
    @Override
    Two build() {
      throw new AssertionError();
    }
  }
}

class Three<E extends Three<E>> extends Two.B<E> {
  static Three<?> c() {
    throw new AssertionError();
  }

  E g() {
    throw new AssertionError();
  }
}
