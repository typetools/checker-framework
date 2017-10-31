// :: error: (initialization.fields.uninitialized)
public class Issue783c<T> {
    private T val;

    public void set(T val) {
        this.val = val;
    }

    public T get() {
        return val;
    }
}
