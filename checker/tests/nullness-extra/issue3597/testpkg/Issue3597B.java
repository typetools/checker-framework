package testpkg;

import org.checkerframework.checker.nullness.qual.Nullable;

public class Issue3597B {
    @Nullable Object f() {
        return new Object();
    }
}
