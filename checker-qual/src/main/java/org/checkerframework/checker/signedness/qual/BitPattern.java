package org.checkerframework.checker.signedness.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * The value represents a bit pattern that should not be interpreted as a signed or unsigned number.
 * It may only be used in bitwise operations, not in arithmetic operations.
 *
 * <p>This annotation is typically used on the return type of methods like {@code
 * Double.doubleToLongBits()} and {@code Float.floatToIntBits()}, and on the parameter type of
 * methods like {@code Double.longBitsToDouble()} and {@code Float.intBitsToFloat()}.
 *
 * @checker_framework.manual #signedness-checker Signedness Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({UnknownSignedness.class})
public @interface BitPattern {}
