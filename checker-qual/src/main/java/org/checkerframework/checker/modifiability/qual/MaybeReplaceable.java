package org.checkerframework.checker.modifiability.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.DefaultQualifierInHierarchy;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * The top qualifier in the Replace hierarchy. The collection may or may not support replace
 * operations such as {@code set} and {@code replaceAll}. The checker conservatively issues an error
 * wherever a replace operation is called on a {@code @MaybeReplaceable} expression.
 *
 * <p>This is the default qualifier for unannotated types in the Replace hierarchy.
 *
 * @checker_framework.manual #modifiability-checker Modifiability Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({})
@DefaultQualifierInHierarchy
public @interface MaybeReplaceable {}
