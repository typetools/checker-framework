// Test case for Issue 1347.
// https://github.com/typetools/checker-framework/issues/1347

public class Issue1347<T> {
    T t;
    T t2;
    Object o;

    Issue1347(T t) {
        this(t, 0);
    }

    Issue1347(T t, int i) {
        this.t = t;
        this.t2 = t;
        this.o = new Object();
    }

    // :: error: (initialization.fields.uninitialized)
    Issue1347(T t, String s) {
        this.t = t;
        this.o = new Object();
    }
}
