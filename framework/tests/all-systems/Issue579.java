// Test case for Issue579
// https://github.com/typetools/checker-framework/issues/579
class Issue579{

    public <T>  void foo(Generic<T> real, Generic<? super T> other, boolean flag) {
        bar(flag ? real : other);
    }

    <Q> void bar(Generic<? extends Q> param) {
    }
    interface Generic<F> {
    }
}
