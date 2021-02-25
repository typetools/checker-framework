package org.checkerframework.common.aliasing.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.DefaultQualifierInHierarchy;
import org.checkerframework.framework.qual.InvisibleQualifier;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * Temporary type qualifier:
 *
 * <p>This is the default type qualifier for the Leaked hierarchy.
 *
 * <p>Once the stub parser gets updated to read non-type-qualifier annotations on stub files (Issue
 * 383), this annotation can be removed, and {@link NonLeaked} and {@link LeakedToResult} can be
 * made to be type annotations but not type qualifiers and not in a type hierarchy.
 *
 * @checker_framework.manual #aliasing-checker Aliasing Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({})
@DefaultQualifierInHierarchy
@SubtypeOf({LeakedToResult.class})
@InvisibleQualifier
public @interface MaybeLeaked {}
