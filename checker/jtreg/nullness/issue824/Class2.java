/*
 * @test
 * @summary Test case for Issue 824 https://github.com/typetools/checker-framework/issues/824
 * @compile -XDrawDiagnostics -Xlint:unchecked ../issue824lib/Class1.java
 * @compile -XDrawDiagnostics -Xlint:unchecked -processor org.checkerframework.checker.nullness.NullnessChecker -AprintErrorStack -Anomsgtext Class2.java -Astubs=Class1.astub -AstubWarnIfNotFound
 * @compile/fail/ref=Class2NoStub.out -XDrawDiagnostics -Xlint:unchecked -processor org.checkerframework.checker.nullness.NullnessChecker -AprintErrorStack -Anomsgtext Class2.java
 */

import org.checkerframework.checker.nullness.qual.Nullable;

public class Class2<X> extends Class1<X> {
    void call(Class1<@Nullable X> class1, Gen<@Nullable X> gen) {
        class1.methodTypeParam(null);
        class1.classTypeParam(null);

        class1.wildcardExtends(gen);
        class1.wildcardSuper(gen);
    }

    @Override
    public <T> T methodTypeParam(T t) {
        return super.methodTypeParam(t);
    }
}
