/*
 * @test
 * @summary Test case for Issue 1565 https://github.com/typetools/checker-framework/issues/1565
 *
 * @compile -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -Astubs=MyStub.astub -AstubWarnIfNotFound MyListGeneric.java
 */

import java.util.AbstractList;

public abstract class MyListGeneric<T> extends AbstractList<T> {
    public MyListGeneric() {
        clear();
    }
}
