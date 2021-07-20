// Test case for Issue 1838:
// https://github.com/typetools/checker-framework/issues/1838

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;
import java.util.List;

public class Issue1838Min {
    List<List<@Nullable Object>> llno = new ArrayList<>();
    // :: error: (assignment.type.incompatible)
    List<? extends List<Object>> lweo = llno;
}
