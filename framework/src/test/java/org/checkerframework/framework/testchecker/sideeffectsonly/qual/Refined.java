package org.checkerframework.framework.testchecker.sideeffectsonly.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * Bottom qualifier of a toy type system. The toy type system is used to test whether dataflow
 * analysis correctly type-refines methods annotated with {@code @SideEffectsOnly}.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({Unrefined.class})
public @interface Refined {}
