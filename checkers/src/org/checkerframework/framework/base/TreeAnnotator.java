package org.checkerframework.framework.base;

import javax.lang.model.type.TypeMirror;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TreeVisitor;

import checkers.types.AnnotatedTypeMirror;


public class TreeAnnotator<Q> {
    public QualifiedTypeMirror<Q> visitBinary(BinaryTree node, TypeMirror type) {
        return null;
    }

    public QualifiedTypeMirror<Q> visitLiteral(LiteralTree node, TypeMirror type) {
        return null;
    }
}
