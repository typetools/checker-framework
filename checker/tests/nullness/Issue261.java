// Test case for Issue 261
// https://github.com/typetools/checker-framework/issues/261
class Issue261 {
    boolean b;
    // :: error: (initialization.fields.uninitialized)
    class Flag<T> {
        T value;
    }

    static <T> T getValue(Flag<T> flag) {
        return flag.value;
    }

    Issue261(Flag<Boolean> flag) {
        this.b = getValue(flag);
    }
}
