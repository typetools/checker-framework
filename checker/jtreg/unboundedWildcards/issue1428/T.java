/*
 * @test
 * @summary Test for Issue 1428.
 * https://github.com/typetools/checker-framework/issues/1428
 *
 * @compile B.java
 * @compile -XDrawDiagnostics -processor org.checkerframework.checker.tainting.TaintingChecker T.java
 */

import java.util.Collections;
import java.util.List;

public class T {
  public List<L<?>> f(B b) {
    return true ? Collections.<L<?>>emptyList() : b.getItems();
  }
}
