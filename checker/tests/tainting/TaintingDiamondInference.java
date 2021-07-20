// Test case for issue #660: https://github.com/typetools/checker-framework/issues/660

import org.checkerframework.checker.tainting.qual.Untainted;

import java.util.Set;
import java.util.TreeSet;

public class TaintingDiamondInference {

    private @Untainted Set<@Untainted String> s;

    public TaintingDiamondInference() {
        // :: warning: (cast.unsafe.constructor.invocation)
        s = new @Untainted TreeSet<>();
        // :: warning: (cast.unsafe.constructor.invocation)
        s = new @Untainted TreeSet<@Untainted String>();
    }
}
