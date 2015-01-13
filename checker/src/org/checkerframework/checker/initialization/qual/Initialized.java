package org.checkerframework.checker.initialization.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.NullnessChecker;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifierInHierarchy;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TypeQualifier;

/**
 * This type qualifier belongs to the freedom-before-commitment initialization
 * tracking type-system. This type-system is not used on its own, but in
 * conjunction with some other type-system that wants to ensure safe
 * initialization. For instance, {@link NullnessChecker} uses
 * freedom-before-commitment to track initialization of {@link NonNull} fields.
 *
 * <p>
 * This type qualifier indicates that the object has been fully initialized;
 * reading fields from such objects is fully safe and yields objects of the
 * correct type.
 *
 * @checker_framework.manual #nullness-checker Nullness Checker
 */
@TypeQualifier
@SubtypeOf(UnknownInitialization.class)
@DefaultQualifierInHierarchy
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE_USE, ElementType.TYPE_PARAMETER })
public @interface Initialized {
}
