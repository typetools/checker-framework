package org.checkerframework.checker.tainting.classic.qual;

import java.lang.annotation.*;

import org.checkerframework.checker.tainting.classic.TaintingClassicChecker;
import org.checkerframework.framework.qual.*;

/**
 * The top qualifier of the tainting type system.
 * This annotation is associated with the {@link TaintingClassicChecker}.
 *
 * @see Untainted
 * @see TaintingClassicChecker
 * @checker_framework.manual #tainting-checker Tainting Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@DefaultQualifierInHierarchy
@SubtypeOf({})
public @interface Tainted {}
