// Minimized test case from InitializationVisitor.

class IATF<
        Value extends CFAV<Value>,
        Store extends IS<Value, Store>,
        Transfer extends IT<Value, Transfer, Store>,
        Flow extends CFAA<Value, Store, Transfer>>
    extends GATF<Value, Store, Transfer, Flow> {}

class CFAV<V extends CFAV<V>> {}

class IS<V extends CFAV<V>, S extends IS<V, S>> extends CFAS<V, S> {}

class IT<V extends CFAV<V>, T extends IT<V, T, S>, S extends IS<V, S>> extends CFAT<V, S, T> {}

class CFAA<V extends CFAV<V>, S extends CFAS<V, S>, T extends CFAT<V, S, T>> {}

class CFAT<V extends CFAV<V>, S extends CFAS<V, S>, T extends CFAT<V, S, T>> {}

class CFAS<V extends CFAV<V>, S extends CFAS<V, S>> {}

class GATF<
    Value extends CFAV<Value>,
    Store extends CFAS<Value, Store>,
    TransferFunction extends CFAT<Value, Store, TransferFunction>,
    FlowAnalysis extends CFAA<Value, Store, TransferFunction>> {}

class BTV<Factory extends GATF<?, ?, ?, ?>> {}

class IV<
        Factory extends IATF<Value, Store, ?, ?>,
        Value extends CFAV<Value>,
        Store extends IS<Value, Store>>
    extends BTV<Factory> {}
