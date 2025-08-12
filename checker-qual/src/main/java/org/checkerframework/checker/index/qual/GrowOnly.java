package org.checkerframework.checker.index.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * An expression of type {@code @GrowOnly} may not be used to remove elements, e.g., by calling
 * {@code remove()} or {@code clear()} on it. The underlying collection is guaranteed not to shrink
 * via operations on aliases.
 *
 * @checker_framework.manual #growonly-checker Grow-only Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({UnshrinkableRef.class})
public @interface GrowOnly {}
