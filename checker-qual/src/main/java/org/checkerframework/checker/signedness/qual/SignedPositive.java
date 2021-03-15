package org.checkerframework.checker.signedness.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The expression's value is in the signed positive range; that is, its most significant bit is not
 * set. The value has the same interpretation as {@link Signed} and {@link Unsigned} &mdash; both
 * interpretations are equivalent.
 *
 * <p>Programmers should rarely write {@code @SignedPositive}. Instead, the programmer should write
 * {@link Signed} or {@link Unsigned} to indicate how the programmer intends the value to be
 * interpreted.
 *
 * <p>Internally, this is translated to the {@code @}{@link SignednessGlb} annotation. This means
 * that programmers do not see this annotation in error messages.
 *
 * @see SignednessGlb
 * @checker_framework.manual #signedness-checker Signedness Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface SignedPositive {}
