package org.checkerframework.checker.experimental.tainting_qual.qual;

import java.lang.annotation.*;

import org.checkerframework.checker.experimental.tainting_qual.TaintingChecker;
import org.checkerframework.framework.qual.*;

/**
 * The top qualifier of the tainting type system.
 * This annotation is associated with the {@link TaintingChecker}.
 *
 * @see Untainted
 * @see TaintingChecker
 * @checker_framework.manual #tainting-checker Tainting Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@DefaultQualifierInHierarchy
@SubtypeOf({})
public @interface Tainted {}
