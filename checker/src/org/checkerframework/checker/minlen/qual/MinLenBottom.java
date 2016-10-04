package org.checkerframework.checker.minlen.qual;

import java.lang.annotation.*;
import org.checkerframework.checker.minlen.qual.*;
import org.checkerframework.framework.qual.*;

/**
 *  The bottom type for MinLen. Assigned to null, etc.
 */
@SubtypeOf(MinLen.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@ImplicitFor(
    literals = {LiteralKind.NULL},
    typeNames = {java.lang.Void.class}
)
public @interface MinLenBottom {}
