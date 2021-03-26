package wildcard;

public class Viz {

  static class AbstractValue<A extends AbstractValue<A>> {}

  public interface Store<B extends Store<B>> {}

  public interface TransferFunction<C extends AbstractValue<C>, D extends Store<D>> {}

  public interface CFGVisualizer<
      E extends AbstractValue<E>, F extends Store<F>, G extends TransferFunction<E, F>> {}

  static class CFAbstractStore<V extends AbstractValue<V>, X extends CFAbstractStore<V, X>>
      implements Store<X> {

    void test(CFGVisualizer<?, X, ?> param) {}
  }
}
