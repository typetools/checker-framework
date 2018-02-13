// Testcase for Issue 1749
// https://github.com/typetools/checker-framework/issues/1749
abstract class Issue1749 {

    public interface A {}

    interface B extends A {}

    public class I<X> {}

    abstract <Y> I<Y> f(Class<? super Y> x);

    void f() {
        I<B> x = f(A.class);
    }
}
