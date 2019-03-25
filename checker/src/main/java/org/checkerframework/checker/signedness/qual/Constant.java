package org.checkerframework.checker.signedness.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.ImplicitFor;
import org.checkerframework.framework.qual.LiteralKind;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * The value is a compile-time constant, and may be interpreted as {@link Signed} or {@link
 * Unsigned}.
 *
 * <p>This annotation is automatically applied to manifest literals. It is also applied to
 * compile-time constants whose most significant bit is not set (that is, they have the same
 * interpretation as signed or unsigned).
 *
 * @checker_framework.manual #signedness-checker Signedness Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({Unsigned.class, Signed.class})
@ImplicitFor(literals = {LiteralKind.INT, LiteralKind.LONG, LiteralKind.CHAR})
public @interface Constant {}
