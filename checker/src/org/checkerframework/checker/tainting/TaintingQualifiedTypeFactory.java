package org.checkerframework.checker.tainting;

import java.util.ArrayList;

import javax.lang.model.type.TypeMirror;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.Tree;

import org.checkerframework.qualframework.base.DefaultQualifiedTypeFactory;
import org.checkerframework.qualframework.base.QualifierHierarchy;
import org.checkerframework.qualframework.base.QualifiedTypeMirror;
import org.checkerframework.qualframework.base.QualifiedTypeMirror.QualifiedDeclaredType;
import org.checkerframework.qualframework.base.TreeAnnotator;
import org.checkerframework.qualframework.util.ExtendedTypeMirror;

import org.checkerframework.checker.qualparam.QualifierParameterHierarchy;
import org.checkerframework.checker.qualparam.QualifierParameterTypeFactory;
import org.checkerframework.checker.qualparam.QualParams;

public class TaintingQualifiedTypeFactory extends QualifierParameterTypeFactory<Tainting> {
    @Override
    protected QualifierHierarchy<Tainting> createGroundQualifierHierarchy() {
        return new TaintingQualifierHierarchy();
    }

    @Override
    protected TaintingAnnotationConverter createAnnotationConverter() {
        return new TaintingAnnotationConverter();
    }

    @Override
    protected TreeAnnotator<QualParams<Tainting>> createTreeAnnotator() {
        return new TreeAnnotator<QualParams<Tainting>>() {
            @Override
            public QualifiedTypeMirror<QualParams<Tainting>> visitLiteral(LiteralTree tree, ExtendedTypeMirror type) {
                if (tree.getKind() == Tree.Kind.STRING_LITERAL) {
                    return new QualifiedDeclaredType<QualParams<Tainting>>(
                            type, new QualParams<>("Main", Tainting.UNTAINTED), new ArrayList<>());
                } else {
                    return super.visitLiteral(tree, type);
                }
            }
        };
    }
}
