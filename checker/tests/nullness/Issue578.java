// Test case for issue #578: https://github.com/typetools/checker-framework/issues/578
public class Issue578 {
  <A, B> void eval(Helper<B> helper, Interface<A> anInterface) {
    Object o = new SomeGenericClass<>(helper.helperMethod(anInterface));
  }
}

abstract class Helper<C> {
  abstract <D> Interface<C> helperMethod(Interface<D> anInterface);
}

interface Interface<E> {}

final class SomeGenericClass<F> {
  SomeGenericClass(Interface<F> s) {}
}
