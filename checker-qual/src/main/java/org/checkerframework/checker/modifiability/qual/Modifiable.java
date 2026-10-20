package org.checkerframework.checker.modifiability.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Convenience alias usually meaning {@code @Growable @Shrinkable @Replaceable @SeqGrowable}.
 * Calling a mutating operation (growing, shrinking, replacing, or sequenced-growing) on this
 * collection will not throw {@link UnsupportedOperationException}.
 *
 * <p>As an exception, {@code @Modifiable} means {@code @Maybe*} if the type does not support a
 * given category of operation; for example, {@code @Modifiable Iterator} means
 * {@code @MaybeGrowable @Shrinkable @MaybeReplaceable @MaybeSeqGrowable Iterator}.
 *
 * @checker_framework.manual #modifiability-checker Modifiability Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface Modifiable {}
