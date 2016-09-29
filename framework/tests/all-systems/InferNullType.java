class InferNullType {

    <T extends Object> T toInfer(T input) {
        return input;
    }

    <T> T toInfer2(T input) {
        return input;
    }

    void x() {
        @SuppressWarnings("nullness:type.argument.type.incompatible")
        Object m = toInfer(null);
        Object m2 = toInfer2(null);
    }
}
