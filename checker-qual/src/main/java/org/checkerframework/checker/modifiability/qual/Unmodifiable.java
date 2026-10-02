package org.checkerframework.checker.modifiability.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Convenience alias usually meaning
 * {@code @Ungrowable @SeqUngrowable @Unshrinkable @Unreplaceable}. Calling a mutating operation
 * (growing, sequenced-growing, shrinking, or replacing) will throw {@link
 * UnsupportedOperationException}.
 *
 * <p>As an exception, {@code @Unmodifiable} means {@code @Maybe*} if the type does not support a
 * given category of operation; for example, {@code @Unmodifiable Iterator} means
 * {@code @MaybeGrowable @MaybeSeqGrowable @Unshrinkable @MaybeReplaceable Iterator}.
 *
 * @checker_framework.manual #modifiability-checker Modifiability Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface Unmodifiable {}
