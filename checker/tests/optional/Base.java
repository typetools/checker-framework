public class Base {

  static class OneClass<A extends ThreeClass<A, B>, B extends OneClass<A, B>> {
    TwoClass<A, ?, ?> get() {
      return null;
    }
  }

  static class TwoClass<
      C extends ThreeClass<C, D>, D extends SubOneClass<C, D, E>, E extends TwoClass<C, D, E>> {}

  static class ThreeClass<F extends ThreeClass<F, G>, G extends OneClass<F, G>> {}

  class SubOneClass<
          H extends ThreeClass<H, I>, I extends SubOneClass<H, I, J>, J extends TwoClass<H, I, J>>
      extends OneClass<H, I> {}
}
