package org.checkerframework.common.aliasing.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * This annotation is used on a formal parameter to indicate that the parameter is not leaked
 * (stored in a location that could be accessed later) nor returned by the method body.
 *
 * <p>For example, the parameter of {@link String#String(String s)} is {@code @NonLeaked}, because
 * the method only uses the parameter to make a copy of it.
 *
 * <p>This annotation is currently trusted, not checked.
 *
 * @see LeakedToResult
 * @checker_framework.manual #aliasing-checker Aliasing Checker
 */

// This is a type qualifier because of a Checker Framework limitation (Issue 383), but its hierarchy
// is ignored. Once the stub parser gets updated to read non-type-qualifiers annotations on stub
// files, this annotation won't be a type qualifier anymore.

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE})
@SubtypeOf({})
public @interface NonLeaked {}
