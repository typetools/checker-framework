/*
 * @test
 * @summary Test case for Issue 824 https://github.com/typetools/checker-framework/issues/824
 * @ignore
 * @compile -XDrawDiagnostics -Xlint:unchecked -processor org.checkerframework.checker.nullness.NullnessChecker -AprintErrorStack -Anomsgtext NoStubFirst.java NoStubSecond.java
 * @compile -XDrawDiagnostics -Xlint:unchecked First.java
 * @compile -XDrawDiagnostics -Xlint:unchecked -processor org.checkerframework.checker.nullness.NullnessChecker -AprintErrorStack -Anomsgtext Second.java -Astubs=First.astub
*/

class First {
  public interface Supplier<T> {}
  public interface Callable<T> {}
  public static <T> void method(Supplier<T> supplier, Callable<? super T> callable) {}
}
