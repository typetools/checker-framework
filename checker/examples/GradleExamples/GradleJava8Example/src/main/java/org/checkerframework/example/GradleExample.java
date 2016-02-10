package org.checkerframework.example;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import org.apache.commons.lang3.text.StrBuilder;

import java.util.List;
import java.util.ArrayList;

/**
 * If you run:
 * gradle build
 *
 * The build for this project should fail with a warning for the line:
 * @NonNull Object nn = nullable;
 *
 * And this line:
 * list.add(null);
 */
public class GradleExample {

    public static @Nullable Object nullable = null;

    public static void main(final String[] args) {
        System.out.println("Hello World!");

        StrBuilder stb = new StrBuilder();

        @NonNull Object nn = nullable;  // error on this line
        System.out.println(nn);
        List<@NonNull String> list = new ArrayList<>();
        list.add(null); // error on this line
    }
}
