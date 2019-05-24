package org.checkerframework.example;

import org.apache.commons.lang3.text.StrBuilder;
import org.checkerframework.common.value.qual.IntVal;

/**
 * If you run:
 *
 * <pre>mvn compile</pre>
 *
 * The build for this project should fail with a warning for the line:
 *
 * <pre>@NonNull Object nn = nullable;</pre>
 */
public class MavenExample {

    public static @IntVal(5) int five = 5;

    public static void main(final String[] args) {
        System.out.println("Hello World!");

        StrBuilder stb = new StrBuilder();

        @IntVal(55) int l = five; // error on this line
        System.out.println(l);
    }
}
