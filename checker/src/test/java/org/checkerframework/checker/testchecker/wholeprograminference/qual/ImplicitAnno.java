package org.checkerframework.checker.testchecker.wholeprograminference.qual;

import org.checkerframework.framework.qual.DefaultFor;
import org.checkerframework.framework.qual.IgnoreInWholeProgramInference;
import org.checkerframework.framework.qual.SubtypeOf;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Toy type system for testing field inference.
 *
 * @see Sibling1, Sibling2, Parent
 */
@SubtypeOf({Sibling1.class, Sibling2.class, SiblingWithFields.class})
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@IgnoreInWholeProgramInference
@DefaultFor(types = java.lang.StringBuffer.class)
public @interface ImplicitAnno {}
