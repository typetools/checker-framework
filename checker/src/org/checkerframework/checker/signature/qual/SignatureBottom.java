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
@TargetLocations({DefaultLocation.EXPLICIT_LOWER_BOUNDS,
    DefaultLocation.EXPLICIT_UPPER_BOUNDS})
@ImplicitFor(trees = {Tree.Kind.NULL_LITERAL},
  typeNames = {java.lang.Void.class})
@DefaultFor({DefaultLocation.LOWER_BOUNDS})
public @interface SignatureBottom {}
