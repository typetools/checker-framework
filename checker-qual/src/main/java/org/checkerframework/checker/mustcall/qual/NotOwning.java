package org.checkerframework.checker.mustcall.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation indicating that ownership should not be transferred to the annotated parameter, field,
 * or method's call sites, for the purposes of Must Call checking. For a full description of the
 * semantics, see the documentation of {@link Owning}.
 *
 * <p>Formal parameters and fields are {@link NotOwning} by default. Method return types are
 * {@code @Owning} by default. Constructor results are always {@code @Owning}.
 *
 * <p>When the {@code -AnoLightweightOwnership} command-line argument is passed to the checker, this
 * annotation and {@link Owning} are ignored.
 *
 * @checker_framework.manual #resource-leak-checker Resource Leak Checker
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD})
public @interface NotOwning {}
