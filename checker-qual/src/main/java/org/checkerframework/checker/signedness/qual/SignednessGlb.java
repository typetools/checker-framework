package org.checkerframework.checker.signedness.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * Client code may interpret the value either as {@link Signed} or as {@link Unsigned}. This
 * primarily applies to values whose most significant bit is not set (i.e., {@link SignedPositive}),
 * and thus the value has the same interpretation as signed or unsigned.
 *
 * <p><b>The programmer should not write this annotation.</b> Instead, the programmer should write
 * {@link Signed} or {@link Unsigned} to indicate how the programmer intends the value to be
 * interpreted. For a value whose most significant bit is not set and different clients may treat it
 * differently (say, the return value of certain library routines, or certain constant fields), the
 * programmer should write {@code @}{@link SignedPositive} instead of {@code @SignednessGlb}.
 *
 * <p>The Signedness Checker applies this annotation to manifest literals. This permits a value like
 * {@code -1} or {@code 255} or {@code 0xFF} to be used in both signed and unsigned contexts. The
 * Signedness Checker has no way of knowing how a programmer intended a literal to be used, so it
 * does not issue warnings for any uses of a literal. (An alternate design would require the
 * programmer to explicitly annotate every manifest literal whose most significant bit is set. That
 * might detect more errors, at the cost of much greater programmer annotation effort.)
 *
 * <p>The "Glb" in the name stands for "greatest lower bound", because this type is the greatest
 * lower bound of the types {@link Signed} and {@link Unsigned}; that is, this type is a subtype of
 * both of those types.
 *
 * @see SignedPositive
 * @checker_framework.manual #signedness-checker Signedness Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({Unsigned.class, Signed.class})
public @interface SignednessGlb {}
