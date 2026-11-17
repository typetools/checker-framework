package org.checkerframework.checker.signedness.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * Indicates that a value is not an arithmetic quantity, but rather a pattern of bits -- for
 * example, the result of {@code Double.doubleToLongBits}, or a value used as a bit set.
 *
 * <p>All bitwise operators ({@literal {&, |, ^, ~}}) and all shifts ({@literal {<<, >>, >>>}}) are
 * permitted on a {@code @BitPattern} value. All arithmetic operators ({@literal {+, -, *, /, %}},
 * including the unary and compound-assignment forms) are forbidden, as is string concatenation.
 *
 * <p>{@code @BitPattern} is a subtype of {@code @}{@link UnknownSignedness} and a supertype of
 * {@code @}{@link SignednessBottom}, but is otherwise unrelated to the other qualifiers in this
 * type system.
 *
 * @checker_framework.manual #signedness-checker Signedness Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({UnknownSignedness.class})
public @interface BitPattern {}
