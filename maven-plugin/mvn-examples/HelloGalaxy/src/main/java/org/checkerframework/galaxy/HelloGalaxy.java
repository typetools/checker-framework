package org.checkerframework.galaxy;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import org.apache.commons.lang3.text.StrBuilder;


/**
 * If you run:
 * maven checkers:check
 * or
 * maven package
 *
 * The build for this project should fail with a warning for the line:
 * @NonNull Object nn = nullable;
 *
 * I have included commons lang3 to demonstrate that the Checker Framework Maven plugin
 * will indeed find the correct Maven classpath.
 */
public class HelloGalaxy {

    public static @Nullable Object nullable = null;

    public static void main(final String[] args) {
        System.out.println("Hello Galaxy!");

        StrBuilder stb = new StrBuilder();

        @NonNull Object nn = nullable;  //error on this line
        System.out.println(nn);
    }

}
