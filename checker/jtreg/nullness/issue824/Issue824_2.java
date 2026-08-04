/*
 * @test
 * @summary Test case for Issue 824 https://github.com/typetools/checker-framework/issues/824
 * The defaults for type variable upper bounds with type Object changed since
 * the issue was filed.  So, this test case has been changed so that
 * annotations on type variable bounds in stub files is still tested.
 * @compile -Xlint:unchecked ../issue824lib/Issue824_1.java
 * @compile/fail/ref=Issue824_2.out -XDrawDiagnostics -Xlint:unchecked -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext Issue824_2.java -Astubs=Issue824_1.astub
 * @compile -Xlint:unchecked -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext Issue824_2.java
 */

import org.checkerframework.checker.nullness.qual.Nullable;

public class Issue824_2<X> extends Issue824_1<X> {
  void call(Issue824_1<@Nullable X> class1, Gen<@Nullable X> gen) {
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
