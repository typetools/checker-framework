package org.checkerframework.checkers.tainting;

import java.util.ArrayList;

import javax.lang.model.type.TypeMirror;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.Tree;

import org.checkerframework.framework.base.DefaultQualifiedTypeFactory;
import org.checkerframework.framework.base.QualifierHierarchy;
import org.checkerframework.framework.base.QualifiedTypeMirror;
import org.checkerframework.framework.base.QualifiedTypeMirror.QualifiedDeclaredType;
import org.checkerframework.framework.base.TreeAnnotator;
import org.checkerframework.framework.util.ExtendedTypeMirror;

public class TaintingQualifiedTypeFactory extends DefaultQualifiedTypeFactory<Tainting> {
    @Override
    protected QualifierHierarchy<Tainting> createQualifierHierarchy() {
        return new TaintingQualifierHierarchy();
    }

    @Override
    protected TaintingAnnotationConverter createAnnotationConverter() {
        return new TaintingAnnotationConverter();
    }

    @Override
    protected TreeAnnotator<Tainting> createTreeAnnotator() {
        return new TreeAnnotator<Tainting>() {
            @Override
            public QualifiedTypeMirror<Tainting> visitLiteral(LiteralTree tree, ExtendedTypeMirror type) {
                if (tree.getKind() == Tree.Kind.STRING_LITERAL) {
                    return new QualifiedDeclaredType<Tainting>(
                            type, Tainting.UNTAINTED, new ArrayList<>());
                } else {
                    return super.visitLiteral(tree, type);
                }
            }
        };
    }
}
