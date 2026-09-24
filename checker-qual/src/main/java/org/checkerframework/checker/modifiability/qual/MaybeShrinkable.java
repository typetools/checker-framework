package org.checkerframework.checker.modifiability.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.DefaultQualifierInHierarchy;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * The top qualifier in the Shrink hierarchy. The collection may or may not support shrink
 * operations such as {@code remove}, {@code, removeAll}, and {@code clear}. The checker
 * conservatively issues an error wherever a shrink operation is called on a
 * {@code @MaybeShrinkable} expression.
 *
 * <p>This is the default qualifier for unannotated types in the Shrink hierarchy.
 *
 * @checker_framework.manual #modifiability-checker Modifiability Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({})
@DefaultQualifierInHierarchy
public @interface MaybeShrinkable {}
