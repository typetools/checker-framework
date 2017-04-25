// Test case for Issue 880:
// https://github.com/typetools/checker-framework/issues/880

import java.io.Serializable;
import java.util.List;
import org.checkerframework.checker.nullness.qual.NonNull;

class Issue880Test implements Serializable {}

class Issue880Use {
    void foo() {
        Issue880Test other = null;
    }
}

abstract class Issue880TestSub extends Issue880Test implements List<@NonNull String> {}

class Issue880SubUse {
    void foo() {
        Issue880TestSub other = null;
    }
}
