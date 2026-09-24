package org.checkerframework.checker.modifiability.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.DefaultQualifierInHierarchy;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * The top qualifier in the sequenced-grow hierarchy. The collection or map may or may not support
 * positional operations such as {@code addFirst}, {@code addLast}, {@code putFirst}, and {@code
 * putLast}. The checker conservatively issues an error wherever a sequenced-grow operation is
 * called on a {@code @MaybeSeqGrowable} expression.
 *
 * <p>This is the default qualifier for unannotated types in the sequenced-grow hierarchy.
 *
 * @checker_framework.manual #modifiability-checker Modifiability Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({})
@DefaultQualifierInHierarchy
public @interface MaybeSeqGrowable {}
