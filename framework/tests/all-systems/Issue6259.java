@SuppressWarnings("unchecked")
public class Issue6259 {
  public interface A {
    interface Builder<AT extends A> {
      AT build();
    }
  }

  public abstract static class B implements C {
    public abstract static class Builder<X> implements C.Builder<X, B> {}
  }

  public interface I<CT extends C, CBT extends C.Builder<?, ? extends CT>> {}

  public interface C {
    interface Builder<X, T extends C> {}
  }

  public static final class L implements I<B, B.Builder<String>> {}

  static class S<AT extends A, RT, CT extends C, CBT extends C.Builder<RT, CT>> {

    public static <
            ET extends A, E2 extends A.Builder<ET>, CT, IT extends C, I2 extends C.Builder<CT, IT>>
        S<ET, CT, IT, I2> assemble(I<IT, I2>... xs) {
      throw new AssertionError();
    }
  }

  void f(L f) {
    S<A, String, B, B.Builder<String>> storeStack = S.assemble(f);
    var storeStackVar = S.assemble(f);
    var storeStackVarExplicit = S.<A, A.Builder<A>, String, B, B.Builder<String>>assemble(f);
  }
}
