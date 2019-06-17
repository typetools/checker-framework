package org.checkerframework.checker.signedness.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.index.qual.Positive;
import org.checkerframework.common.value.qual.IntRange;
import org.checkerframework.framework.qual.LiteralKind;
import org.checkerframework.framework.qual.QualifierForLiterals;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * Client code may interpret the value either as {@link Signed} or as {@link Unsigned} -- both
 * interpretations are legal. This primarily applies to values whose most significant bit is not
 * set, and thus the value has the same interpretation as signed or unsigned.
 *
 * <p>As a special case, the Signedness Checker also applies this annotation to manifest literals.
 * This permits a value like {@code -1} or {@code 255} or {@code 0xFF} to be used in both signed and
 * unsigned contexts. The Signedness Checker has no way of knowing how a programmer intended a
 * literal to be used, so it does not issue warnings for any uses of a literal. (An alternate design
 * would require the programmer to explicitly annotate every manifest literal whose most significant
 * bit is set. That might detect more errors, but it would cause much greater programmer annotation
 * effort.)
 *
 * <p>The programmer should not write this annotation, because for each variable the programmer
 * knows how the value is intended to be interpreted. Instead of this annotation, the programmer
 * should write {@link Signed} or {@link Unsigned} to indicate how the programmer intends the value
 * to be interpreted. The programmer can also annotate values as @{@link NonNegative}, @{@link
 * Positive}, or @{@link IntRange}(from=<em>someNonNegativeConstant</em>).
 *
 * <p>The "Glb" in the name stands for "greatest lower bound", because this type is the greatest
 * lower bound of the types {@link Signed} and {@link Unsigned}; that is, this type is a subtype of
 * both of those types.
 *
 * @checker_framework.manual #signedness-checker Signedness Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({Unsigned.class, Signed.class})
@QualifierForLiterals({LiteralKind.INT, LiteralKind.LONG, LiteralKind.CHAR})
public @interface SignednessGlb {}
