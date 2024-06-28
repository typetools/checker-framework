package org.checkerframework.checker.nonempty.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * The {@link java.util.Collection Collection}, {@link java.util.Iterator Iterator}, {@link
 * java.lang.Iterable Iterable}, or {@link java.util.Map Map} is definitely non-empty.
 *
 * @checker_framework.manual #non-empty-checker Non-Empty Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_PARAMETER, ElementType.TYPE_USE})
@SubtypeOf(UnknownNonEmpty.class)
public @interface NonEmpty {}
