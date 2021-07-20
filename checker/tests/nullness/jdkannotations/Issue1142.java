// Issue 1142 https://github.com/typetools/checker-framework/issues/1142

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.concurrent.ConcurrentHashMap;

public class Issue1142 {

    void foo() {
        // :: error: (type.argument.type.incompatible)
        ConcurrentHashMap<Integer, @Nullable Integer> chm1 = new ConcurrentHashMap<>();
        chm1.put(1, null);
    }
}
