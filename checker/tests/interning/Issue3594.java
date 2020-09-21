class Issue3594 {

    // Throwable is annotated with @UsesObjectEquals, which is an inherited annotation.
    // So, MyThrowable should be treated as @UsesObjectEquals, too.
    static class MyThrowable extends Throwable {}

    void use(MyThrowable t, MyThrowable t2) {
        boolean b = t == t2;
    }
}
