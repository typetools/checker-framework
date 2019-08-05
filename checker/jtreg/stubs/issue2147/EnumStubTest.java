/*
 * @test
 * @summary Test case for Issue 2147 https://github.com/typetools/checker-framework/issues/2147
 *
 * @compile -XDrawDiagnostics SampleEnum.java
 *
 * @compile/fail/ref=WithoutStub.out -XDrawDiagnostics -processor org.checkerframework.checker.tainting.TaintingChecker -AstubWarnIfNotFound EnumStubTest.java
 * @compile/fail/ref=WithStub.out -XDrawDiagnostics -processor org.checkerframework.checker.tainting.TaintingChecker -AstubWarnIfNotFound -Astubs=SampleEnum.astub EnumStubTest.java
 */

import org.checkerframework.checker.tainting.qual.*;

class EnumStubTest {
    void test() {
        requireEnum(SampleEnum.FIRST);
        requireEnum(SampleEnum.SECOND);
    }

    void requireEnum(@Untainted SampleEnum sEnum) {}
}
