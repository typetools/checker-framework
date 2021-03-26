/*
 * @test
 * @summary Test case for Issue 824 https://github.com/typetools/checker-framework/issues/824
 * The defaults for type variable upper bounds with type Object changed since
 * the issue was filed.  So, this test case has been changed so that
 * annotations on type variable bounds in stub files is still tested.
 * @compile -XDrawDiagnostics -Xlint:unchecked ../issue824lib/Class1.java
 * @compile/fail/ref=Class2.out -XDrawDiagnostics -Xlint:unchecked -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext Class2.java -Astubs=Class1.astub -AstubWarnIfNotFound
 * @compile -XDrawDiagnostics -Xlint:unchecked -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext Class2.java
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
