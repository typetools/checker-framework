// @below-java9-jdk-skip-test
class Issue3408 {
    final String foo;

    String getFoo() {
        return foo;
    }

    Issue3408() {
        var anon =
                new Object() {
                    String bar() {
                        // :: error: (method.invocation.invalid)
                        return Issue3408.this.getFoo().substring(1);
                    }
                };
        anon.bar(); // / WHOOPS... NPE, `getFoo()` returns `foo` which is still null
        this.foo = "Hello world";
    }
}
