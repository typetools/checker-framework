package org.checkerframework.checker.signature.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

import org.checkerframework.framework.qual.*;

import com.sun.source.tree.Tree;

/**
 * Represents the bottom of the type-qualifier hierarchy.
 *
 * @checker_framework.manual #signature-checker Signature Checker
 */
@SubtypeOf({SourceNameForNonArray.class,
    FieldDescriptorForArray.class,
    MethodDescriptor.class
    })
@Target({ElementType.TYPE_USE})
@TargetLocations({ TypeUseLocation.EXPLICIT_LOWER_BOUNDS,
    TypeUseLocation.EXPLICIT_UPPER_BOUNDS})
@ImplicitFor(trees = {Tree.Kind.NULL_LITERAL},
  typeNames = {java.lang.Void.class})
@DefaultFor({ TypeUseLocation.LOWER_BOUNDS})
public @interface SignatureBottom {}
