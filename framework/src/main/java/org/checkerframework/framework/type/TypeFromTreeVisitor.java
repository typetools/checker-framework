package org.checkerframework.framework.type;

import com.sun.source.tree.Tree;
import com.sun.source.util.SimpleTreeVisitor;
import org.checkerframework.javacutil.BugInCF;

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
            throw new BugInCF("TypeFromTree.defaultAction: null tree");
        }
        throw new BugInCF(
                "TypeFromTree.defaultAction: conversion undefined for tree type "
                        + node.getKind()
                        + "\n");
    }
}
