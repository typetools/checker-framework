import java.util.function.Function;

class Issue3598 {

    static class DClass extends EClass {}

    static class EClass<F> {}

    // Must be Function, can't use interface defined in this class.
    static class XClass<P> implements Function<P, P> {

        @Override
        public P apply(P protoT) {
            return protoT;
        }

        // DClass extends a raw class.
        static Function<DClass, DClass> f(DClass k) {
            // Crash on this line.
            return new XClass<>(k);
        }

        XClass(P p) {}
    }
}
