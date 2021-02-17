// Test case for Issue 3128:
// https://github.com/typetools/checker-framework/issues/3128

public class Issue3128 {
    class One<S> {}

    class Two<U extends Number, V extends One<U>> {}

    One<Two<?, ?>> foo() {
        return new One<>();
    }
}
