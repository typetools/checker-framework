package org.checkerframework.checker.tainting;

import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import org.checkerframework.qualframework.base.QualifiedTypeMirror;
import org.checkerframework.qualframework.base.QualifierHierarchy;
import org.checkerframework.qualframework.base.SetQualifierVisitor;
import org.checkerframework.qualframework.base.TypeVariableSubstitutor;
import org.checkerframework.qualframework.poly.CombiningOperation;
import org.checkerframework.qualframework.poly.PolyQual;
import org.checkerframework.qualframework.poly.PolyQual.GroundQual;
import org.checkerframework.qualframework.poly.QualParams;
import org.checkerframework.qualframework.poly.QualifiedParameterTypeVariableSubstitutor;
import org.checkerframework.qualframework.poly.QualifierParameterTreeAnnotator;
import org.checkerframework.qualframework.poly.QualifierParameterTypeFactory;
import org.checkerframework.qualframework.poly.Wildcard;
import org.checkerframework.qualframework.util.ExtendedTypeMirror;
import org.checkerframework.qualframework.util.QualifierContext;

public class TaintingQualifiedTypeFactory extends QualifierParameterTypeFactory<Tainting> {

    public TaintingQualifiedTypeFactory(QualifierContext<QualParams<Tainting>> context) {
        super(context);
    }

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
                    result = SetQualifierVisitor.apply(result, new QualParams<>(new GroundQual<>(Tainting.UNTAINTED)));
                } else if (tree.getKind() == Kind.NULL_LITERAL) {
                    return SetQualifierVisitor.apply(result, TaintingQualifiedTypeFactory.this.getQualifierHierarchy().getBottom());
                }
                return result;
            }
        };
    }

    private CombiningOperation<Tainting> lubOp = new CombiningOperation.Lub<>(new TaintingQualifierHierarchy());

    @Override
    public TypeVariableSubstitutor<QualParams<Tainting>> createTypeVariableSubstitutor() {
        return new QualifiedParameterTypeVariableSubstitutor<Tainting>() {
            @Override
            protected Wildcard<Tainting> combineForSubstitution(Wildcard<Tainting> a, Wildcard<Tainting> b) {
                return a.combineWith(b, lubOp, lubOp);
            }

            @Override
            protected PolyQual<Tainting> combineForSubstitution(PolyQual<Tainting> a, PolyQual<Tainting> b) {
                return a.combineWith(b, lubOp);
            }
        };
    }
}
