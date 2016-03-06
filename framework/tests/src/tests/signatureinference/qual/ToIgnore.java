package tests.signatureinference.qual;

import org.checkerframework.framework.qual.IgnoreInSignatureInference;

/**
 * Toy type system for testing field inference.
 * @see Sibling1, Sibling2, Parent
 */
@IgnoreInSignatureInference
public @interface ToIgnore {}
