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

    // TODO: It is a bug that this is not reported as an unused suppression
    @SuppressWarnings("index:foo.bar.baz")
    void method7() {}

    // TODO: It is a bug that this is not reported as an unused suppression
    @SuppressWarnings("allcheckers:purity.not.deterministic.call")
    void method8() {}
}
