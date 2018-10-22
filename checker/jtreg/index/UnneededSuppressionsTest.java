/*
 * @test
 * @summary Test -AwarnUnneededSuppressions
 *
 * @compile/ref=UnneededSuppressionsTest.out -XDrawDiagnostics -processor org.checkerframework.checker.index.IndexChecker -AwarnUnneededSuppressions UnneededSuppressionsTest.java
 */

import org.checkerframework.checker.index.qual.NonNegative;

@SuppressWarnings("index")
public class UnneededSuppressionsTest {

    void method(@NonNegative int i) {
        @SuppressWarnings("index")
        @NonNegative int x = i - 1;
    }

    void method2() {
        @SuppressWarnings("fallthrough")
        int x = 0;
    }

    @SuppressWarnings({"tainting", "lowerbound"})
    void method3() {
        @SuppressWarnings("upperbound:assignment.type.incompatible")
        int z = 0;
    }

    void method4() {
        @SuppressWarnings("lowerbound:assignment.type.incompatible")
        @NonNegative int x = -1;
    }

    @SuppressWarnings("purity.not.deterministic.call")
    void method5() {}

    @SuppressWarnings("purity")
    void method6() {}
}
