package org.checkerframework.checker.testchecker.ainfer.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.DefaultFor;
import org.checkerframework.framework.qual.IgnoreInWholeProgramInference;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * Toy type system for testing field inference.
 *
 * @see AinferSibling1, AinferSibling2, AinferParent
 */
@SubtypeOf({AinferSibling1.class, AinferSibling2.class, AinferSiblingWithFields.class})
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@IgnoreInWholeProgramInference
@DefaultFor(types = java.lang.StringBuffer.class)
public @interface AinferImplicitAnno {}
