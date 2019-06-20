package org.checkerframework.checker.signedness.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.index.qual.Positive;
import org.checkerframework.common.value.qual.IntRange;
import org.checkerframework.framework.qual.ImplicitFor;
import org.checkerframework.framework.qual.LiteralKind;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * The value may be interpreted as {@link Signed} or {@link Unsigned}. It is either a manifest
 * literal, or it is a value whose most significant bit is not set (so it has the same
 * interpretation as signed or unsigned)
 *
 * <p>The programmer should not write this annotation; the programmer should always write {@link
 * Signed} or {@link Unsigned} to indicate how the value is intended to be interpreted. The
 * programmer can also annotate values as @{@link NonNegative}, @{@link Positive}, or @{@link
 * IntRange}(from=<em>someNonNegativeConstant</em>).
 *
 * @checker_framework.manual #signedness-checker Signedness Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({Unsigned.class, Signed.class})
@ImplicitFor(literals = {LiteralKind.INT, LiteralKind.LONG, LiteralKind.CHAR})
public @interface Constant {}
