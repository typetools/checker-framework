package org.checkerframework.qualframework.base;

import javax.lang.model.type.TypeMirror;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TreeVisitor;

import org.checkerframework.framework.type.AnnotatedTypeMirror;

import org.checkerframework.qualframework.util.ExtendedTypeMirror;


/**
 * {@link DefaultQualifiedTypeFactory} component for computing the qualified
 * type of a {@link Tree}.
 */
public class TreeAnnotator<Q> {
    public QualifiedTypeMirror<Q> visitBinary(BinaryTree node, ExtendedTypeMirror type) {
        return null;
    }

    public QualifiedTypeMirror<Q> visitLiteral(LiteralTree node, ExtendedTypeMirror type) {
        return null;
    }
}
