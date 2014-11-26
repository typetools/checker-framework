package org.checkerframework.checker.experimental.tainting_qual_poly;

import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.Tree;

import org.checkerframework.qualframework.base.QualifierHierarchy;
import org.checkerframework.qualframework.base.QualifiedTypeMirror;
import org.checkerframework.qualframework.base.SetQualifierVisitor;
import org.checkerframework.qualframework.util.ExtendedTypeMirror;

import org.checkerframework.qualframework.poly.CombiningOperation;
import org.checkerframework.qualframework.poly.QualifierParameterTypeFactory;
import org.checkerframework.qualframework.poly.QualifierParameterTreeAnnotator;
import org.checkerframework.qualframework.poly.QualParams;
import org.checkerframework.qualframework.poly.Wildcard;

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
    protected QualifierParameterTreeAnnotator<Tainting> createTreeAnnotator() {
        return new QualifierParameterTreeAnnotator<Tainting>(this) {
            @Override
            public QualifiedTypeMirror<QualParams<Tainting>> visitLiteral(LiteralTree tree, ExtendedTypeMirror type) {
                QualifiedTypeMirror<QualParams<Tainting>> result = super.visitLiteral(tree, type);
                if (tree.getKind() == Tree.Kind.STRING_LITERAL) {
                    result = SetQualifierVisitor.apply(result, new QualParams<>("Main", Tainting.UNTAINTED));
                }
                return result;
            }
        };
    }


    private final CombiningOperation<Tainting> lubOp = new CombiningOperation.Lub<>(new TaintingQualifierHierarchy());

    @Override
    protected Wildcard<Tainting> combineForSubstitution(Wildcard<Tainting> a, Wildcard<Tainting> b) {
        return a.combineWith(b, lubOp, lubOp);
    }
}
