package org.checkerframework.checker.nonempty.qual;

import java.lang.annotation.*;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * The {@link java.util.Collection Collection}, {@link java.util.Iterator Iterator}, {@link Iterable
 * Iterable}, or {@link java.util.Map Map}, or {@link java.util.stream.Stream Stream} is definitely
 * non-empty.
 *
 * @checker_framework.manual #non-empty-checker Non-Empty Checker
 */
// Temporary for backward compatibility
// Marked 2024-11-08, Scheduled for Removal: TBD
@Deprecated
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_PARAMETER, ElementType.TYPE_USE})
@SubtypeOf(UnknownNonEmpty.class)
public @interface NonEmpty {}
