// Test case for Issue 1838:
// https://github.com/typetools/checker-framework/issues/1838
// @skip-test Until issue is fixed

import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

class Issue1838Min {
    List<List<@Nullable Object>> llno = new ArrayList<>();
    List<? extends List<Object>> lweo = llno;
}
