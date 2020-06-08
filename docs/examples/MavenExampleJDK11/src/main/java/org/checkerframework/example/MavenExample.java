package org.checkerframework.example;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.text.StrBuilder;
import org.checkerframework.checker.nullness.qual.KeyFor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * If you run:
 *
 * <pre>mvn compile</pre>
 *
 * then the build for this project should fail with a warning for the line:
 *
 * <pre>@NonNull Object nn = nullable;</pre>
 */
public class MavenExample {

    public static @Nullable Object nullable = null;
    public Map<Object, Object> map = new HashMap<>();

    public static void main(final String[] args) {
        System.out.println("Hello World!");

        StrBuilder stb = new StrBuilder();

        @NonNull Object nn = nullable; // error on this line
        System.out.println(nn);
    }

    // Test for -J--add-opens=jdk.compiler/com.sun.tools.javac.comp=ALL-UNNAMED.
    void mapTest(@KeyFor("map") Object k) {}
}
