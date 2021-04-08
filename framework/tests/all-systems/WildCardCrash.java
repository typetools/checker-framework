public class WildCardCrash {}

abstract class AbstractTransfer123<
        IndexStore extends CFAbstractStore123<CFValue123, IndexStore>,
        MySelf extends AbstractTransfer123<IndexStore, MySelf>>
    extends CFAbstractTransfer123<CFValue123, IndexStore, MySelf> {
  void method() {
    // There was a crash when checking the assignment of analysis to the formal parameter.
    SomeGen<IndexStore> rfi = new SomeGen<>(analysis);
  }
}

class SomeGen<IndexStore extends Store123<IndexStore>> {
  public SomeGen(CFAbstractAnalysis123<CFValue123, ?, ?> analysis) {}
}

class CFValue123 extends CFAbstractValue123<CFValue123> {}

@SuppressWarnings({"initialization", "initializedfields:contracts.postcondition.not.satisfied"})
class CFAbstractTransfer123<
        V extends CFAbstractValue123<V>,
        S extends CFAbstractStore123<V, S>,
        T extends CFAbstractTransfer123<V, S, T>>
    implements TransferFunction123<V, S> {
  protected CFAbstractAnalysis123<V, S, T> analysis;
}

class CFAbstractValue123<V extends CFAbstractValue123<V>> implements AbstractValue123<V> {}

class CFAbstractStore123<V extends CFAbstractValue123<V>, S extends CFAbstractStore123<V, S>>
    implements Store123<S> {}

abstract class CFAbstractAnalysis123<
        V extends CFAbstractValue123<V>,
        S extends CFAbstractStore123<V, S>,
        T extends CFAbstractTransfer123<V, S, T>>
    extends Analysis123<V, S, T> {}

interface AbstractValue123<V extends AbstractValue123<V>> {}

interface Store123<T extends Store123<T>> {}

interface TransferFunction123<A extends AbstractValue123<A>, S extends Store123<S>> {}

class Analysis123<
    A extends AbstractValue123<A>, S extends Store123<S>, T extends TransferFunction123<A, S>> {}
