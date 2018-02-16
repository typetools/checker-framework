// Test case for Issue 1712
// https://github.com/typetools/checker-framework/issues/1712

abstract class Issue1712 {

    abstract <T> T match(
            Function<? super SubclassA, T> visitA, Function<? super SubclassB, T> visitB);

    class SubclassA extends Issue1712 {
        @Override
        <T> T match(Function<? super SubclassA, T> visitA, Function<? super SubclassB, T> visitB) {
            return visitA.apply(this); // line 11
        }
    }

    class SubclassB extends Issue1712 {
        @Override
        <T> T match(Function<? super SubclassA, T> visitA, Function<? super SubclassB, T> visitB) {
            return visitB.apply(this); // line 18
        }
    }

    abstract class Function<T1, T2> {
        abstract T2 apply(T1 arg);
    }
}
