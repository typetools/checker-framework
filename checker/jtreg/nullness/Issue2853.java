/*
 * @test
 * @summary Test case for issue #2853: https://github.com/typetools/checker-framework/issues/2853
 *
 * @compile/timeout=30 -XDrawDiagnostics -Xlint:unchecked -processor org.checkerframework.checker.nullness.NullnessChecker Issue2853.java
 */
public class Issue2853 {

  abstract static class A {

    abstract B getB();

    public abstract C getC();

    public abstract Object getD();

    public abstract Object getE();

    public abstract Object getF();

    public abstract Object getG();

    public abstract H getH();
  }

  abstract static class B {}

  abstract static class C {

    abstract Object getI();
  }

  static class I {

    static class J {}
  }

  abstract static class H {

    abstract Object getK();

    abstract Object getL();

    abstract Object getM();

    abstract Object getN();
  }

  abstract static class O {}

  abstract static class J {

    static O f(B b) {
      throw new AssertionError();
    }
  }

  abstract static class K {

    abstract Object getL();
  }

  abstract static class M {

    abstract N getN();
  }

  abstract static class N {

    abstract Object f();
  }

  static class Test {

    static final ImmutableSet<P> C =
        new ImmutableSet.Builder<P>()
            .add(R.c((A x) -> J.f(x.getB())))
            .add(R.c((A x) -> x.getC().getI()))
            .add(R.c((M x) -> x.getN().f()))
            .add(R.c((A x) -> x.getH().getK()))
            .add(R.c((A x) -> x.getH().getM()))
            .add(R.c((A x) -> x.getH().getL()))
            .add(R.c((A x) -> x.getH().getN()))
            .add(R.c((A x) -> x.getD()))
            .add(R.c((A x) -> x.getE()))
            .add(R.c((A x) -> x.getE()))
            .add(R.c((A x) -> x.getG()))
            .add(R.c((K x) -> x.getL()))
            .build();

    interface P {}

    interface Q<U, V> {

      V get(U message);
    }

    private static class R<T, U> implements P {

      static <T, U> R<T, U> c(Q<T, U> x) {
        throw new AssertionError();
      }
    }
  }

  abstract static class ImmutableSet<E> {

    static class Builder<E> {

      public Builder<E> add(E... elements) {
        throw new AssertionError();
      }

      public ImmutableSet<E> build() {
        throw new AssertionError();
      }
    }
  }
}
