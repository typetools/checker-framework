package wildcards;

import wildcards.Crash.GenericAnnotatedTypeFactory;

public class Crash<Factory extends GenericAnnotatedTypeFactory<?, ?, ?, ?>> {
    public abstract static class GenericAnnotatedTypeFactory<
            Value extends CFAbstractValue<Value>,
            Store extends CFAbstractStore<Value, Store>,
            TransferFunction extends CFAbstractTransfer<Value, Store, TransferFunction>,
            FlowAnalysis extends CFAbstractAnalysis<Value, Store, TransferFunction>> {
        abstract Store getRegularExitStore();
    }

    static class CFAbstractValue<V extends CFAbstractValue<V>> {}

    static class CFAbstractStore<V extends CFAbstractValue<V>, S extends CFAbstractStore<V, S>> {}

    abstract static class CFAbstractTransfer<
            V extends CFAbstractValue<V>,
            S extends CFAbstractStore<V, S>,
            T extends CFAbstractTransfer<V, S, T>> {}

    public abstract static class CFAbstractAnalysis<
            V extends CFAbstractValue<V>,
            S extends CFAbstractStore<V, S>,
            T extends CFAbstractTransfer<V, S, T>> {}

    void use(Factory atypeFactory) {
        CFAbstractStore<?, ?> exitStore = atypeFactory.getRegularExitStore();
    }
}
