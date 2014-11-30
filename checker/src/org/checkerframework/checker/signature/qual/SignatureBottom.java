package org.checkerframework.checker.signature.qual;

import java.lang.annotation.Target;

import org.checkerframework.framework.qual.ImplicitFor;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TypeQualifier;

import com.sun.source.tree.Tree;

/**
 * Represents the bottom of the type-qualifier hierarchy.
 * <p>
 *
 * This annotation may not be written in source code; it is an
 * implementation detail of the checker.
 *
 * @checker_framework.manual #signature-checker Signature Checker
 */
@TypeQualifier
@SubtypeOf({SourceNameForNonArray.class,
    FieldDescriptorForArray.class,
    MethodDescriptor.class
    })
@Target({}) // empty target prevents programmers from writing this in a program
@ImplicitFor(trees = {Tree.Kind.NULL_LITERAL},
  typeNames = {java.lang.Void.class})
public @interface SignatureBottom {}
