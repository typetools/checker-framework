// Test case for Issue 1690
// https://github.com/typetools/checker-framework/issues/1690

import java.io.Serializable;

public class Issue1690<T extends Runnable & Serializable> {

    public Issue1690() {}

    // Can be an inner type or in its own file, shouldn't matter.
    public static interface Issue16902<R extends Issue1690> {
        public R getR();
    }
}
