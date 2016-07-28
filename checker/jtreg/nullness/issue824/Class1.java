/*
 * @test
 * @summary Test case for Issue 824 https://github.com/typetools/checker-framework/issues/824
 * @compile -XDrawDiagnostics -Xlint:unchecked Class1.java
 * @compile -XDrawDiagnostics -Xlint:unchecked -processor org.checkerframework.checker.nullness.NullnessChecker -AprintErrorStack -Anomsgtext Class2.java -Astubs=Class1.astub
 */

public class Class1<Q> {
    class Gen<S> {}

    public <T> T methodTypeParam(T t) {
        return t;
    }

    public void classTypeParam(Q e) {}

    public <F> void wildcardExtends(Gen<? extends F> class1) {}

    public <F> void wildcardSuper(Gen<? super F> class1) {}
}
