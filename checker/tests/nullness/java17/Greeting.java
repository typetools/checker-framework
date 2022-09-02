// @below-java16-jdk-skip-test
// Test case for https://github.com/typetools/checker-framework/issues/5039
package com.example.hello_world;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Objects;

public final class Greeting {
    public final @Nullable String name;

    public Greeting(@Nullable String name) {
        this.name = name;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        return o == this || o instanceof Greeting that && Objects.equals(name, that.name);
    }

    @Override
    public String toString() {
        return name == null ? "World" : name;
    }
}
