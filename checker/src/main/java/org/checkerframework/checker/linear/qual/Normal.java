package org.checkerframework.checker.linear.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.DefaultFor;
import org.checkerframework.framework.qual.DefaultQualifierInHierarchy;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TypeUseLocation;

/**
 * The default qualifier for the hierarchy. Denotes that object can be used any number of times.
 *
 * @checker_framework.manual #linear-checker Linear Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf(Unusable.class)
@DefaultQualifierInHierarchy
@DefaultFor(TypeUseLocation.LOCAL_VARIABLE)
public @interface Normal {}
