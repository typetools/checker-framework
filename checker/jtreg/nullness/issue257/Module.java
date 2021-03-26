/*
 * @test
 * @summary Test for Issue 257
 *
 * @compile -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker Small.java
 *
 * @compile -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker ClientBuilder.java Module.java
 *
 * @compile -XDrawDiagnostics ClientBuilder.java
 */
public class Module {
  void buildClient() {
    ClientBuilder<?> builder = ClientBuilder.newBuilder().setThing().setThing();
  }

  void smaller() {
    ClientBuilder<? extends ClientBuilder<? extends ClientBuilder<? extends ClientBuilder<?>>>>
        builder = ClientBuilder.newBuilder();
  }
}
