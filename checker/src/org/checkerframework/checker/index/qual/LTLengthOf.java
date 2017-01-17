package org.checkerframework.checker.index.qual;

import java.lang.annotation.*;
import org.checkerframework.framework.qual.*;

/**
 * Indicates that the expression annotated with this type qualifier is less than the length of the
 * listed arrays.
 *
 * @checker_framework.manual #index-checker Index Checker
 */
@SubtypeOf(LTEqLengthOf.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface LTLengthOf {
    public String[] value();
}
