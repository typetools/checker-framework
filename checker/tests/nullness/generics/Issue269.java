// Test case for Issue 269
// https://github.com/typetools/checker-framework/issues/269
class Repro {
    // Implicitly G has bound @Nullable Object
    interface Callback<G> {
        public boolean handler(G arg);
    }

    void method1(Callback callback) {
        // Allow this call.
        //:: warning: [unchecked] unchecked call to handler(G) as a member of the raw type Repro.Callback
        callback.handler(this);
    }

    // Implicitly H has bound @NonNull Object
    interface CallbackNN<H extends Object> {
        public boolean handler(H arg);
    }

    void method2(CallbackNN callback) {
        // Forbid this call, because the bound is not respected.
        // TODO: false negative. See #635.
        ////:: error: (argument.type.incompatible)
        //:: warning: [unchecked] unchecked call to handler(H) as a member of the raw type Repro.CallbackNN
        callback.handler(null);
    }
}
