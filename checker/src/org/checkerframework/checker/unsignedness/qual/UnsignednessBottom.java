package org.checkerframework.checker.unsignedness.qual;

import java.lang.annotation.*;

import org.checkerframework.framework.qual.*;

import javax.lang.model.type.TypeKind;

import com.sun.source.tree.Tree;

/**
 * UnsignednessBottom is the bottom qualifier in the Unsigned Type
 * System, and is only assigned to a value in error.
 *
 * @checker_framework.manual #unsignedness-checker Unsignedness Checker
 */
@Target({ElementType.TYPE_USE})
@SubtypeOf( { Constant.class } )
@ImplicitFor(
    trees = { Tree.Kind.NULL_LITERAL },
    types = { TypeKind.NULL }
    )
public @interface UnsignednessBottom { }
