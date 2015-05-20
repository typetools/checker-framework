/*
 * @test
 * @summary Test for Issue 257 
 *
 * @compile -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -AprintErrorStack Small.java
 *
 * @compile -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -AprintErrorStack ClientBuilder.java Module.java -AunsafeUntyped
 *
 * @compile -XDrawDiagnostics ClientBuilder.java
 * @compile/fail/ref=Module-err.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext -AprintErrorStack Module.java -AunsafeUntyped
 */
class Module {
  void buildClient() {
    ClientBuilder<?> builder = ClientBuilder.newBuilder()
        .setThing()
        .setThing();
  }

  void smaller() {
    ClientBuilder<? extends ClientBuilder<? extends ClientBuilder<? extends ClientBuilder<?>>>> builder = ClientBuilder.newBuilder();
  }
}
