package org.checkerframework.checker.index.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * An expression of type {@code @UncheckedShrinkable} may be used to remove elements, e.g., by
 * calling {@code remove()} or {@code clear()} on it.
 *
 * <p>The Index Checker does not issue warnings about possible {@code IndexOutOfBoundsException}s
 * when the collection has type {@code UncheckedShrinkable}.
 *
 * <p>Thus, {@code @UncheckedShrinkable} is combination of {@code @}{@link Shrinkable} and a warning
 * suppression. It is particularly useful when first annotating a codebase, to temporarily suppress
 * some warnings while focusing on others.
 *
 * @checker_framework.manual #index-checker Index Checker
 * @checker_framework.manual #growonly-checker Grow-only Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({Shrinkable.class})
public @interface UncheckedShrinkable {}
