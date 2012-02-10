package checkers.signature.quals;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

import checkers.quals.PolymorphicQualifier;
import checkers.quals.TypeQualifier;

/**
 * A polymorphic qualifier for the Signature type system.
 *
 * <p>
 * Any method written using {@code @PolySignature} conceptually has two versions:
 * one in which every instance of {@code @PolySignature String} has been replaced
 * by {@code @Signature String}, and one in which every instance of
 * {@code @PolySignature String} has been replaced by {@code String}.
 */
@Documented
@TypeQualifier
@PolymorphicQualifier
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface PolySignature {}
