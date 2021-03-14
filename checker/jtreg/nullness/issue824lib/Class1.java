public class Class1<Q> {
  class Gen<S> {}

  public <T> T methodTypeParam(T t) {
    return t;
  }

  public void classTypeParam(Q e) {}

  public <F> void wildcardExtends(Gen<? extends F> class1) {}

  public <F> void wildcardSuper(Gen<? super F> class1) {}
}
