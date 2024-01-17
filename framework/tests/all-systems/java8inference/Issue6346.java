import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class Issue6346 {

  public abstract static class A<X extends A<X, Y>, Y extends A.Builder<X, Y>> extends B<X, Y> {

    public abstract static class Builder<X1 extends A<X1, Y1>, Y1 extends Builder<X1, Y1>>
        extends B.Builder<X1, Y1> {}
  }

  public abstract static class B<X2 extends B<X2, Y2>, Y2 extends B.Builder<X2, Y2>> implements C {

    public abstract static class Builder<X3 extends B<X3, Y3>, Y3 extends Builder<X3, Y3>>
        implements C.Builder {}
  }

  public interface C extends D {
    interface Builder extends D, Cloneable {}
  }

  public interface D {}

  abstract static class E<L extends A<L, ?>, I, O> extends F<L> {

    public abstract static class Builder<L extends A<L, ?>, I, O> {
      public <T> Builder<L, I, O> g(Function<I, T> x, BiConsumer<O, T> y) {
        throw new AssertionError();
      }

      public abstract Builder<L, I, O> version(int version);
    }

    public static <
            L extends A<L, B>,
            B extends A.Builder<L, B>,
            I extends A<I, O>,
            O extends A.Builder<I, O>>
        Builder<L, I, O> f(L x, Function<L, List<I>> y, Supplier<O> z, BiConsumer<B, I> z1) {
      throw new AssertionError();
    }
  }

  public interface G extends D {}

  abstract static class H extends A<H, H.Builder> implements G {

    public List<I> h() {
      throw new AssertionError();
    }

    public static final class Builder extends A.Builder<H, Builder> implements G {

      private Builder i(I value) {
        return this;
      }
    }

    public static H j() {
      throw new AssertionError();
    }
  }

  public interface J extends D {}

  public static final class I extends A<I, I.Builder> implements J {

    public long k() {
      return 0L;
    }

    public static final class Builder extends A.Builder<I, I.Builder> implements J {

      Builder l(long value) {
        return this;
      }
    }

    public static Builder n() {
      throw new AssertionError();
    }
  }

  abstract static class F<L extends A<L, ?>> {}

  void f() {
    var x = E.f(H.j(), H::h, I::n, H.Builder::i).g(I::k, I.Builder::l);
  }
}
