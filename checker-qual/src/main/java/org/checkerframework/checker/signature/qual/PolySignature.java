package org.checkerframework.checker.signature.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.PolymorphicQualifier;

/**
 * A polymorphic qualifier for the Signature type system.
 *
 * <p>Any method written using {@code @PolySignature} conceptually has two versions: one in which
 * every instance of {@code @PolySignature String} has been replaced by {@code @Signature String},
 * and one in which every instance of {@code @PolySignature String} has been replaced by {@code
 * String}.
 *
 * @checker_framework.manual #signature-checker Signature Checker
 * @checker_framework.manual #qualifier-polymorphism Qualifier polymorphism
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@PolymorphicQualifier(SignatureUnknown.class)
public @interface PolySignature {}
