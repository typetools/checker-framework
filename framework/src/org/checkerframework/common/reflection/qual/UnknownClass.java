package org.checkerframework.common.reflection.qual;

import java.lang.annotation.Target;

import org.checkerframework.framework.qual.DefaultQualifierInHierarchy;
import org.checkerframework.framework.qual.InvisibleQualifier;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TypeQualifier;

/**
 * Represents a Class object whose run-time value is not known at compile time.
 * Also represents non-Class values.
 * <p>
 *
 * This annotation is the default in the hierarchy and may not be written in
 * source code.
 *
 * @checker_framework.manual #classval-checker ClassVal Checker
 */
@TypeQualifier
@InvisibleQualifier
@SubtypeOf({})
@Target({})
@DefaultQualifierInHierarchy
public @interface UnknownClass {
}
