
class CFAbstractValue<V extends CFAbstractValue<V>> {}

//having a concrete extension of CFAbstractValue is the key difference between this and
//InferTypeArgs1.java.  In this case we end up comparing CFValue with V extends CFAbstractValue<V>
//which kicks off the DefaultRawnessComparer.  Before I fixed it, it then blew the stack
class CFValue extends CFAbstractValue<CFValue> {
    public CFValue(CFAbstractAnalysis<CFValue, ?, ?> analysis) {
    }
}

class CFAbstractStore<V extends CFAbstractValue<V>, S extends CFAbstractStore<V, S>>{}
class CFAbstractTransfer<V extends CFAbstractValue<V>,
        S extends CFAbstractStore<V, S>,
        T extends CFAbstractTransfer<V, S, T>> {}

class CFAbstractAnalysis<V extends CFAbstractValue<V>, 
        S extends CFAbstractStore<V,S>,
        T extends CFAbstractTransfer<V,S,T>> {
    public CFValue defaultCreateAbstractValue(CFAbstractAnalysis<CFValue, ?, ?> analysis) {
        return new CFValue(analysis);
    }
}
