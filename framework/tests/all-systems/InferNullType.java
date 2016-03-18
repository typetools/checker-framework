class InferNullType {

    <T extends Object> T toInfer(T input) {
        return input;
    }

    void x() {
        @SuppressWarnings("nullness:type.argument.type.incompatible")
        Object m = toInfer(null);
    }
}