package org.checkerframework.checker.minlen.qual;

import java.lang.annotation.*;
import org.checkerframework.checker.minlen.qual.*;
import org.checkerframework.framework.qual.*;

/**
 *  The bottom type for the MinLen type system.
 *
 * @checker_framework.manual #index-checker Index Checker
 */
@SubtypeOf(MinLen.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@ImplicitFor(literals = {LiteralKind.NULL})
public @interface MinLenBottom {}
