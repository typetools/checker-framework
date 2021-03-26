interface Crash8Lib {
  interface MyIterable<A> extends Iterable<A> {}

  interface Box<B extends Box<B>> {}

  interface Root<C extends Root<C, D>, D> {
    C get1();

    MyIterable<D> getIterable2();
  }

  interface Sub<F extends Root<F, G>, G, H extends Box<H>> extends Root<F, G> {}

  interface Leaf<I extends Root<I, J>, J extends Box<J>> extends Sub<I, J, J> {}

  interface Main {
    <K extends Box<K>> Leaf<?, K> foo(Class<K> b);
  }
}
