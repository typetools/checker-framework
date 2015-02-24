package org.checkerframework.qualframework.poly;

import java.util.*;

import javax.lang.model.element.Element;
import javax.lang.model.type.TypeKind;
import com.sun.source.tree.*;

import org.checkerframework.qualframework.base.TreeAnnotator;
import org.checkerframework.qualframework.base.SetQualifierVisitor;
import org.checkerframework.qualframework.base.QualifiedTypeMirror;
import org.checkerframework.qualframework.base.QualifiedTypeMirror.QualifiedDeclaredType;
import org.checkerframework.qualframework.util.ExtendedTypeMirror;

/** {@code TreeAnnotator} instance for qualifier parameter checkers. */
public class QualifierParameterTreeAnnotator<Q> extends TreeAnnotator<QualParams<Q>> {
    private QualifierParameterTypeFactory<Q> factory;

    public QualifierParameterTreeAnnotator(QualifierParameterTypeFactory<Q> factory) {
        super();
        this.factory = factory;
    }

    // These overrides are intended to avoid adding invalid qualifier
    // parameters due to the Checker Framework's default annotation handling.
    // In particular, we want to prevent the framework from adding more
    // qualifier parameters than the type actually declares, such as producing
    // the type "C<<Q=Regex(1), R=Regex(2)>>" when C only declares "C<<Q>>".

    @Override
    public QualifiedTypeMirror<QualParams<Q>> visitCompoundAssignment(CompoundAssignmentTree node, ExtendedTypeMirror type) {
        QualifiedTypeMirror<QualParams<Q>> result = super.visitCompoundAssignment(node, type);
        return filterParams(result);
    }

    @Override
    public QualifiedTypeMirror<QualParams<Q>> visitBinary(BinaryTree node, ExtendedTypeMirror type) {
        QualifiedTypeMirror<QualParams<Q>> result = super.visitBinary(node, type);
        return filterParams(result);
    }

    @Override
    public QualifiedTypeMirror<QualParams<Q>> visitUnary(UnaryTree node, ExtendedTypeMirror type) {
        QualifiedTypeMirror<QualParams<Q>> result = super.visitUnary(node, type);
        return filterParams(result);
    }

    @Override
    public QualifiedTypeMirror<QualParams<Q>> visitConditionalExpression(ConditionalExpressionTree node, ExtendedTypeMirror type) {
        QualifiedTypeMirror<QualParams<Q>> result = super.visitConditionalExpression(node, type);
        return filterParams(result);
    }

    @Override
    public QualifiedTypeMirror<QualParams<Q>> visitTypeCast(TypeCastTree node, ExtendedTypeMirror type) {
        QualifiedTypeMirror<QualParams<Q>> result = super.visitTypeCast(node, type);
        return filterParams(result);
    }

    /** Filter the {@link QualParams} on a {@link QualifiedTypeMirror} to
     * ensure that only the type's declared parameters are present.
     */
    private QualifiedTypeMirror<QualParams<Q>> filterParams(QualifiedTypeMirror<QualParams<Q>> type) {
        if (type.getKind() != TypeKind.DECLARED) {
            return type;
        }

        QualifiedDeclaredType<QualParams<Q>> declType = (QualifiedDeclaredType<QualParams<Q>>)type;

        Element elt = declType.getUnderlyingType().asElement();
        Set<String> validParams = factory.getAnnotationConverter().getDeclaredParameters(
                elt, factory.getDeclAnnotations(elt), factory.getDecoratedElement(elt));

        if (validParams.equals(type.getQualifier().keySet())) {
            return type;
        }

        Map<String, Wildcard<Q>> params = new HashMap<>(type.getQualifier());
        params.keySet().retainAll(validParams);
        return SetQualifierVisitor.apply(type, new QualParams<>(params, type.getQualifier().getPrimary()));
    }
}
