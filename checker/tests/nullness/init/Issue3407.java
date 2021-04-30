// @below-java9-jdk-skip-test
public class Issue3407 {
  final String foo;

  String getFoo() {
    return foo;
  }

  Issue3407() {
    var anon =
        new Object() {
          String bar() {
            // :: error: (method.invocation)
            return Issue3407.this.getFoo().substring(1);
          }
        };
    anon.bar(); // / WHOOPS... NPE, `getFoo()` returns `foo` which is still null
    this.foo = "Hello world";
  }
}
