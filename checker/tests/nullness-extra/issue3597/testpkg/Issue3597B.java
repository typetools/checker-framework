package testpkg;

import org.checkerframework.checker.nullness.qual.Nullable;

class Issue3597B {
    @Nullable Object f() {
        return new Object();
    }
}
