// Version of framework/tests/all-systems/InferNullType.java with expected Nullness Checker warnings
class InferNullType {

    <T extends Object> T toInfer(T input) {
        return input;
    }

    <T> T toInfer2(T input) {
        return input;
    }

    <T, S extends T> T toInfer3(T input, S p2) {
        return input;
    }

    <T extends Number, S extends T> T toInfer4(T input, S p2) {
        return input;
    }

    void x() {
        // :: error: (type.argument.type.incompatible)
        Object m = toInfer(null);
        Object m2 = toInfer2(null);

        Object m3 = toInfer3(null, null);
        Object m4 = toInfer3(1, null);
        Object m5 = toInfer3(null, 1);

        // :: error: (type.argument.type.incompatible)
        Object m6 = toInfer4(null, null);
        // :: error: (type.argument.type.incompatible)
        Object m7 = toInfer4(1, null);
        // :: error: (type.argument.type.incompatible)
        Object m8 = toInfer4(null, 1);
    }
}
