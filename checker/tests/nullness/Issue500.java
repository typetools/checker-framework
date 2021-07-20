// Test case for Issue 500:
// https://github.com/typetools/checker-framework/issues/500

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;

public class Issue500<M> {
    // Tests GLB
    public Issue500(@Nullable List<M> list) {
        if (list instanceof ArrayList<?>) {}
    }
    // Tests GLB
    public Issue500(@Nullable AbstractList<M> list) {
        if (list instanceof ArrayList<?>) {}
    }

    // Tests LUB
    void foo(
            @Nullable AbstractList<M> l1,
            ArrayList<?> l2,
            @Nullable AbstractList<?> list,
            boolean b) {
        list = b ? l1 : l2;
    }
}
