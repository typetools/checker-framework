// Test case for Issue #2082:
// https://github.com/typetools/checker-framework/issues/2082

import java.util.concurrent.Callable;

class Issue2082 {
    Callable foo = () -> 0;
}
