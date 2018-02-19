// Test case for issue 1775
// https://github.com/typetools/checker-framework/issues/1775

@SuppressWarnings("") // just check for crashes
class Issue1775 {
    interface Box<A> {
        <B extends A> B get();
    }

    <S extends String> Box<S[]> getBox() {
        return null;
    }

    void m() {
        for (String s : getBox().get()) {}
    }
}
