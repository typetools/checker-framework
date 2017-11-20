class InferTypeArgs2<V extends InferTypeArgs2<V>> {}

// having a concrete extension of InferTypeArgs2 is the key difference between this and
// InferTypeArgs1.java.  In this case we end up comparing CFValue with V extends InferTypeArgs2<V>
// which kicks off the DefaultRawnessComparer.  Before I fixed it, it then blew the stack
class CFValue extends InferTypeArgs2<CFValue> {
    public CFValue(InferTypeArgsAnalysis<CFValue, ?, ?> analysis) {}
}

class CFAbstractStore<V extends InferTypeArgs2<V>, S extends CFAbstractStore<V, S>> {}

class CFAbstractTransfer<
        V extends InferTypeArgs2<V>,
        S extends CFAbstractStore<V, S>,
        T extends CFAbstractTransfer<V, S, T>> {}

class InferTypeArgsAnalysis<
        V extends InferTypeArgs2<V>,
        S extends CFAbstractStore<V, S>,
        T extends CFAbstractTransfer<V, S, T>> {
    public CFValue defaultCreateAbstractValue(InferTypeArgsAnalysis<CFValue, ?, ?> analysis) {
        return new CFValue(analysis);
    }
}
