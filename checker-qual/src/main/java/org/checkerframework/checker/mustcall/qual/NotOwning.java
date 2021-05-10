package org.checkerframework.checker.mustcall.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation indicating that ownership should not be transferred to the annotated parameter, field,
 * or (when this is written on a method) return type, for the purposes of Must Call checking.
 *
 * <p>Parameters and fields are treated as if they have this annotation by default unless they have
 * {@link Owning}.
 *
 * <p>When the -AnoLightweightOwnership command-line argument is passed to the checker, this
 * annotation and {@link Owning} are ignored.
 *
 * @checker_framework.manual #resource-leak-checker Resource Leak Checker
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD})
public @interface NotOwning {}
