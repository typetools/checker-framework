/*
 * @test
 * @summary
 * Test case for eisop issue 244
 * https://github.com/eisop/checker-framework/issues/244
 *
 * @compile/fail/ref=LocateArtificialTree.out -XDrawDiagnostics -Xlint:unchecked -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext LocateArtificialTree.java
 *
 */

import org.checkerframework.checker.nullness.qual.*;

import java.util.List;
import java.util.function.Consumer;

public class LocateArtificialTree {
    @NonNull class A {}

    void foo() {
        Consumer<List<@Nullable A>> c = a -> {};
    }
}
