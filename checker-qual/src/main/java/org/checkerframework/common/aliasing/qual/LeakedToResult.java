package org.checkerframework.common.aliasing.qual;

import org.checkerframework.framework.qual.SubtypeOf;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used on a formal parameter to indicate that the parameter may be returned, but
 * it is not otherwise leaked. (A parameter is leaked if it is stored in a field where it could be
 * accessed later, and in that case this annotation would not apply.)
 *
 * <p>For example, the receiver parameter of {@link StringBuffer#append(String s)} is annotated as
 * {@code @LeakedToResult}, because the method returns the updated receiver.
 *
 * <p>This annotation is currently trusted, not checked.
 *
 * @see NonLeaked
 * @checker_framework.manual #aliasing-checker Aliasing Checker
 */

// This is a type qualifier because of a Checker Framework limitation (Issue 383), but its hierarchy
// is ignored. Once the stub parser gets updated to read non-type-qualifiers annotations on stub
// files, this annotation won't be a type qualifier anymore.

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE})
@SubtypeOf({NonLeaked.class})
public @interface LeakedToResult {}
