package org.checkerframework.checker.tainting.qual;

import java.lang.annotation.*;

/**
 * Wild is equivalent to the wildcard operator.
 *
 * {@code @Wild MyClass <==> MyClass&laquo;?&raquo;}
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@Repeatable(MultiWild.class)
public @interface Wild {
    /**
     * The name of the qualifier parameter to set.
     */
    String param();
}
