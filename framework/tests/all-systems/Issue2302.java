// Test case for Issue 2302
// https://github.com/typetools/checker-framework/issues/2302

@SuppressWarnings("unchecked")
public class Issue2302 {
    static class StrangeConstructorTypeArgs<V> {
        // The constructor does not use the type parameter V.
        public StrangeConstructorTypeArgs(MyClass<byte[]> abs) {}
    }

    static class MyClass<VALUE> {}

    static StrangeConstructorTypeArgs getStrangeConstructorTypeArgs() {
        // Crash with the diamond operator.
        // Type inference chooses `Object` as the type argument.
        // That is a bug, since it should choose exactly `byte[]`.
        return new StrangeConstructorTypeArgs(new MyClass<>());

        // No crash with an explicit type argument (no diamond operator), no matter what it is.
        // return new StrangeConstructorTypeArgs(new MyClass<byte[]>());
        // return new StrangeConstructorTypeArgs(new MyClass<Integer>());
        // return new StrangeConstructorTypeArgs(new MyClass<Object>());
        // return new StrangeConstructorTypeArgs(new MyClass<@Tainted Object>());
    }
}
