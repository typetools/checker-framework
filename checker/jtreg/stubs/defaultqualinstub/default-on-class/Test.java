import org.checkerframework.checker.nullness.qual.*;

/*
 * @test
 * @summary Defaults applied on class will not be inherited.
 *
 * @compile -XDrawDiagnostics -Xlint:unchecked List.java
 * @compile -XDrawDiagnostics -Xlint:unchecked MutableList.java
 * @compile/fail/ref=test.out -XDrawDiagnostics -Xlint:unchecked -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext -Astubs=list.astub -Werror Test.java
 */

public class Test {
    // l1 has type List<? extends @NonNull Object>
    // mutableList has type MutableList<? extends @Nullable Object>
    void foo(List<?> l1, MutableList<?> mutableList) {
        // retainAll only accepts List with non-null elements
        l1.retainAll(mutableList);

        // l2 has type List<? extends @NonNull Object>
        List<?> l2 = mutableList;
    }
}
