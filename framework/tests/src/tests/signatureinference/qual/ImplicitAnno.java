package tests.signatureinference.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

import org.checkerframework.framework.qual.IgnoreInSignatureInference;
import org.checkerframework.framework.qual.ImplicitFor;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedNullType;

import com.sun.source.tree.LiteralTree;

/**
 * Toy type system for testing field inference.
 * @see Sibling1, Sibling2, Parent
 */
@SubtypeOf({Sibling1.class, Sibling2.class})
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@IgnoreInSignatureInference
@ImplicitFor(
        typeNames = { java.lang.StringBuffer.class })
public @interface ImplicitAnno {}
