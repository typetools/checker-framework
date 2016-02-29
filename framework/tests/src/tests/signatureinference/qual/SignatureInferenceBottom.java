package tests.signatureinference.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

import org.checkerframework.framework.qual.DefaultFor;
import org.checkerframework.framework.qual.TypeUseLocation;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * Toy type system for testing field inference.
 * @see Sibling1, Sibling2, Parent
 */
@SubtypeOf({ImplicitAnno.class})
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@DefaultFor(TypeUseLocation.LOWER_BOUND)
public @interface SignatureInferenceBottom {}
