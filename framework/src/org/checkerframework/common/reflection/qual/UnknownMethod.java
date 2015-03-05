package org.checkerframework.common.reflection.qual;

import java.lang.annotation.Target;

import org.checkerframework.framework.qual.DefaultQualifierInHierarchy;
import org.checkerframework.framework.qual.InvisibleQualifier;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TypeQualifier;

/**
 * Represents a {@link java.lang.reflect.Method Method} or
 * {@link java.lang.reflect.Constructor Constructor} expression whose
 * run-time value is not known at compile time.  Also represents
 * non-Method, non-Constructor values.
 * <p>
 *
 * This annotation is the default in the hierarchy and may not be written in
 * source code.
 *
 * @checker_framework.manual #methodval-checker MethodVal Checker
 */
@TypeQualifier
@InvisibleQualifier
@SubtypeOf({})
@Target({})
@DefaultQualifierInHierarchy
public @interface UnknownMethod {
}
