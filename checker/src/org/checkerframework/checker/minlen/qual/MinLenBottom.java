package org.checkerframework.checker.minlen.qual;

import java.lang.annotation.*;
import org.checkerframework.framework.qual.*;

/**
 * The bottom type for the MinLen type system.
 *
 * @checker_framework.manual #index-checker Index Checker
 */
@SubtypeOf(MinLen.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@TargetLocations({TypeUseLocation.EXPLICIT_LOWER_BOUND, TypeUseLocation.EXPLICIT_UPPER_BOUND})
@ImplicitFor(
    literals = {LiteralKind.NULL},
    typeNames = {java.lang.Void.class}
)
public @interface MinLenBottom {}
