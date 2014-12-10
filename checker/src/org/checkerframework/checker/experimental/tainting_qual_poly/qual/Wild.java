package org.checkerframework.checker.experimental.tainting_qual_poly.qual;

import java.lang.annotation.*;

/**
 * Wild is equivalent to the wildcard operator.
 *
 * {@code @Wild MyClass <==> MyClass<<?>>}
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@Repeatable(MultiWild.class)
public @interface
        Wild {
    // The name of the parameter to set.
    String param();
}
