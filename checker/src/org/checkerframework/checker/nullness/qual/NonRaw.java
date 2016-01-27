package org.checkerframework.checker.nullness.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.checkerframework.checker.nullness.NullnessRawnessChecker;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Raw;
import org.checkerframework.framework.qual.DefaultFor;
import org.checkerframework.framework.qual.DefaultLocation;
import org.checkerframework.framework.qual.DefaultQualifierInHierarchy;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * This type qualifier belongs to the rawness initialization tracking
 * type-system. This type-system is not used on its own, but in conjunction with
 * some other type-system that wants to ensure safe initialization. For
 * instance, {@link NullnessRawnessChecker} uses rawness to track initialization
 * of {@link NonNull} fields.
 *
 * <p>
 * This type qualifier indicates that the object has been fully initialized;
 * reading fields from such objects is fully safe and yields objects of the
 * correct type.
 *
 * @checker_framework.manual #nullness-checker Nullness Checker
 */
@SubtypeOf(Raw.class)
@DefaultQualifierInHierarchy
@DefaultFor({DefaultLocation.IMPLICIT_UPPER_BOUNDS, DefaultLocation.IMPLICIT_LOWER_BOUNDS,
             DefaultLocation.EXCEPTION_PARAMETER})
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE_USE, ElementType.TYPE_PARAMETER })
public @interface NonRaw {
}
