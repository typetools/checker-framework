// test cases for #2809
// https://github.com/typetools/checker-framework/issues/2809
// @skip-test before the issue is solved.

import org.checkerframework.checker.interning.qual.Interned;

class Issue2809 {

    void new2(MyType<int @Interned []> t, int @Interned [] non) {
        t.self(new MyType<>(non));
    }

    void new2_1(MyType<int @Interned []> t, int @Interned [] non) {
        t.self(new MyType<int @Interned []>(non));
    }

    void new3(MyType<@Interned MyType<Object>> t, @Interned MyType<Object> non) {
        t.self(new MyType<>(non));
    }

    private class MyType<MyTypeParam2F8A> {
        MyType(MyTypeParam2F8A f2a8) {}

        void self(MyType<MyTypeParam2F8A> myType) {}
    }
}
