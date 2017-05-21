package org.checkerframework.framework.type;

import com.sun.source.tree.Tree;
import com.sun.source.util.SimpleTreeVisitor;
import org.checkerframework.javacutil.ErrorReporter;

/**
 * Converts a Tree into an AnnotatedTypeMirror. This class is abstract and provides 2 important
 * properties to subclasses:
 *
 * <ol>
 *   <li>It implements SimpleTreeVisitor with the appropriate type parameters
 *   <li>It provides a defaultAction that causes all visit methods to abort if the subclass does not
 *       override them
 * </ol>
 *
 * @see org.checkerframework.framework.type.TypeFromTree
 */
abstract class TypeFromTreeVisitor
        extends SimpleTreeVisitor<AnnotatedTypeMirror, AnnotatedTypeFactory> {

    TypeFromTreeVisitor() {}

    @Override
    public AnnotatedTypeMirror defaultAction(Tree node, AnnotatedTypeFactory f) {
        if (node == null) {
            ErrorReporter.errorAbort("TypeFromTree.defaultAction: null tree");
            return null; // dead code
        }
        ErrorReporter.errorAbort(
                "TypeFromTree.defaultAction: conversion undefined for tree type "
                        + node.getKind()
                        + "\n");
        return null; // dead code
    }
}
