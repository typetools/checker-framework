class Issue3407 {
    final String foo;

    String getFoo() {
        return foo;
    }

    Issue3407() {
        Object anon =
                new Object() {
                    String bar() {
                        // :: error: (method.invocation.invalid)
                        return Issue3407.this.getFoo().substring(1);
                    }
                };
        anon.bar(); // / WHOOPS... NPE, `getFoo()` returns `foo` which is still null
        this.foo = "Hello world";
    }
}
