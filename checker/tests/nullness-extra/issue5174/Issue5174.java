// Test case for Issue 5174:
// https://github.com/typetools/checker-framework/issues/5174

class Issue5174Super<S> {
    S methodInner(S in) {
        return in;
    }

    S f;
    static Object sf = "";

    Issue5174Super(S f) {
        this.f = f;
    }
}

class Issue5174Sub<T> extends Issue5174Super<T> {
    Issue5174Sub(T f) {
        super(f);
    }

    void accMethImpl(T in) {
        Object o = methodInner(in);
    }

    void accMethExpl(T in) {
        Object o = this.methodInner(in);
    }

    void accFieldImpl() {
        Object o = f;
    }

    void accFieldExpl() {
        Object o = this.f;
    }

    void accStaticField() {
        Object o;
        o = sf;
        o = Issue5174Sub.sf;
        o = Issue5174Super.sf;
    }

    class SubNested {
        void nestedaccMethImpl(T in) {
            Object o = methodInner(in);
        }

        void nestedaccMethExpl(T in) {
            Object o = Issue5174Sub.this.methodInner(in);
        }

        void nestedaccFieldImpl() {
            Object o = f;
        }

        void nestedaccFieldExpl() {
            Object o = Issue5174Sub.this.f;
        }

        void nestedaccStaticField() {
            Object o;
            o = sf;
            o = Issue5174Sub.sf;
            o = Issue5174Super.sf;
        }
    }
}
