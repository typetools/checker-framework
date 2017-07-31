/*
 * @test
 * @summary Test for Issue 1420.
 * https://github.com/typetools/checker-framework/issues/1420
 *
 * @compile B.java
 * @compile -XDrawDiagnostics -processor org.checkerframework.checker.tainting.TaintingChecker -AprintErrorStack T.java
 */

import java.util.Collections;
import java.util.List;

class T {
    public List<L<?>> f(B b) {
        return true ? Collections.<L<?>>emptyList() : b.getItems();
    }
}
