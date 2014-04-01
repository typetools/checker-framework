package org.checkerframework.qualframework.base;

import javax.lang.model.type.TypeMirror;

import com.sun.source.tree.Tree;
import com.sun.source.util.SimpleTreeVisitor;

import org.checkerframework.framework.type.AnnotatedTypeMirror;

import org.checkerframework.qualframework.util.ExtendedTypeMirror;


/**
 * {@link DefaultQualifiedTypeFactory} component for computing the qualified
 * type of a {@link Tree}.
 */
public class TreeAnnotator<Q> extends SimpleTreeVisitor<QualifiedTypeMirror<Q>, ExtendedTypeMirror> {
    public QualifiedTypeMirror<Q> defaultAction(Tree node, ExtendedTypeMirror type) {
        return null;
    }
}
