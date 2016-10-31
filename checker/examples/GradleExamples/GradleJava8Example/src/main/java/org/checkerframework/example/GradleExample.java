package org.checkerframework.example;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.text.StrBuilder;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * If you run:
 *
 * <pre>gradle build</pre>
 *
 * The build for this project should fail with a warning for the line:
 *
 * <pre>@NonNull Object nn = nullable;</pre>
 *
 * And this line:
 *
 * <pre>list.add(null);</pre>
 */
public class GradleExample {

    public static @Nullable Object nullable = null;

    public static void main(final String[] args) {
        System.out.println("Hello World!");

        StrBuilder stb = new StrBuilder();

        @NonNull Object nn = nullable; // error on this line
        System.out.println(nn);
        List<@NonNull String> list = new ArrayList<>();
        list.add(null); // error on this line
    }
}
