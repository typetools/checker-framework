package org.checkerframework.checker.index.qual;

import java.lang.annotation.*;
import org.checkerframework.framework.qual.*;

/**
 * Indicates that the expression annotated with this type qualifier is less than one less than the
 * length of the listed arrays (LessThanOneMinusLengthOf, which isn't quite right, but has a good
 * acronym).
 *
 * @checker_framework.manual #index-checker Index Checker
 */
@SubtypeOf(LTLengthOf.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface LTOMLengthOf {
    public String[] value();
}
