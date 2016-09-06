package org.checkerframework.checker.tainting.classic.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.DefaultQualifierInHierarchy;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * The top qualifier of the tainting type system.
 * This annotation is associated with the {@link  org.checkerframework.checker.tainting.classic.TaintingClassicChecker}.
 *
 * @see Untainted
 * @see  org.checkerframework.checker.tainting.classic.TaintingClassicChecker
 * @checker_framework.manual #tainting-checker Tainting Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@DefaultQualifierInHierarchy
@SubtypeOf({})
public @interface Tainted {}
