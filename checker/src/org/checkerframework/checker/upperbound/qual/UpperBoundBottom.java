package org.checkerframework.checker.upperbound.qual;

import java.lang.annotation.*;
import org.checkerframework.framework.qual.*;

/**
 * The bottom type for the Upper Bound type system. Programmers should not need to write this type.
 *
 * @checker_framework.manual #index-checker Index Checker
 */
@SubtypeOf(LessThanLength.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface UpperBoundBottom {}
