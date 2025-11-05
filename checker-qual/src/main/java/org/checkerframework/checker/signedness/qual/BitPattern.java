package org.checkerframework.checker.signedness.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * The value represents a bit pattern, not a signed or unsigned number. Bit patterns support bitwise
 * operations but forbid arithmetic operations.
 *
 * <p>This annotation is appropriate for values returned by methods like {@link
 * Double#doubleToLongBits(double)} or used as bitsets. Bitwise operations ({@code &}, {@code |},
 * {@code ^}, {@code ~}) and shift operations ({@code <<}, {@code >>}, {@code >>>}) are permitted,
 * but arithmetic operations ({@code +}, {@code -}, {@code *}, {@code /}, {@code %}) and compound
 * assignments ({@code +=}, {@code -=}, etc.) are forbidden.
 *
 * @checker_framework.manual #signedness-checker Signedness Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({UnknownSignedness.class})
public @interface BitPattern {}
