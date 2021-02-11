public class Issue783c<T> {
    // :: error: (initialization.field.uninitialized)
    private T val;

    public void set(T val) {
        this.val = val;
    }

    public T get() {
        return val;
    }
}
