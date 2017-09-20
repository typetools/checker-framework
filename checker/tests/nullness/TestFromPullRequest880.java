// Test case from pull request 880:
//   https://github.com/typetools/checker-framework/pull/880
// Test might also be relevant to issue 989:
//   https://github.com/typetools/checker-framework/issues/989
// Also note a test that uses multiple compilation units at:
//   checker/jtreg/nullness/annotationsOnExtends/

import java.io.Serializable;
import java.util.List;
import org.checkerframework.checker.nullness.qual.NonNull;

class TFPR880Test implements Serializable {}

class TFPR880Use {
    void foo() {
        TFPR880Test other = null;
    }
}

abstract class TFPR880TestSub extends TFPR880Test implements List<@NonNull String> {}

class TFPR880SubUse {
    void foo() {
        TFPR880TestSub other = null;
    }
}
