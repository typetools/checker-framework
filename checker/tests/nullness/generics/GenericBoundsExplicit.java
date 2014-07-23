package nullness.generics;

import org.checkerframework.checker.nullness.qual.*;

class GenericBoundsExplicit<@NonNull T extends @Nullable Object> {

    T t;
    public void method() {
        String str = t.toString();
    }

    public static void doSomething() {
        final GenericBoundsExplicit<@Nullable String> b = new GenericBoundsExplicit<@Nullable String>();
        b.method();
    }
}
