package org.checkerframework.checker.qualparam;

import java.util.*;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;

import org.checkerframework.javacutil.Pair;

import org.checkerframework.qualframework.base.QualifierHierarchy;
import org.checkerframework.qualframework.base.DefaultQualifiedTypeFactory;
import org.checkerframework.qualframework.base.QualifiedTypeMirror;
import org.checkerframework.qualframework.util.QualifierMapVisitor;
import org.checkerframework.qualframework.util.SetQualifierVisitor;
import org.checkerframework.qualframework.base.QualifiedTypeMirror.QualifiedExecutableType;
import org.checkerframework.qualframework.base.QualifiedTypeMirror.QualifiedTypeVariable;

public abstract class QualifierParameterTypeFactory<Q> extends DefaultQualifiedTypeFactory<QualParams<Q>> {
    QualifierHierarchy<Q> groundHierarchy;

    @Override
    protected abstract QualifierParameterAnnotationConverter<Q> createAnnotationConverter();

    public QualifierParameterAnnotationConverter<Q> getAnnotationConverter() {
        return (QualifierParameterAnnotationConverter<Q>)super.getAnnotationConverter();
    }

    protected abstract QualifierHierarchy<Q> createGroundQualifierHierarchy();

    public QualifierHierarchy<Q> getGroundQualifierHierarchy() {
        if (groundHierarchy == null) {
            groundHierarchy = createGroundQualifierHierarchy();
        }
        return groundHierarchy;
    }

    @Override
    protected QualifierHierarchy<QualParams<Q>> createQualifierHierarchy() {
        return QualifierParameterHierarchy.fromGround(getGroundQualifierHierarchy());
    }

    @Override
    protected QualifierParameterTypeAnnotator<Q> createTypeAnnotator() {
        return new QualifierParameterTypeAnnotator<Q>(getAnnotationConverter(),
                new ContainmentHierarchy<>(new PolyQualHierarchy<>(getGroundQualifierHierarchy())));
    }

    /*
    public final QualParams<Q> applyCaptureConversion(QualParams<Q> objectQual) {
        if (objectQual == null || objectQual == QualParams.<Q>getBottom()
                || objectQual == QualParams.<Q>getTop())
            return objectQual;
        return objectQual.capture();
    }
    */
    public final QualParams<Q> qualifierAsMemberOf(QualParams<Q> memberQual, QualParams<Q> objectQual) {
        if (memberQual == null || memberQual == QualParams.<Q>getBottom()
                || memberQual == QualParams.<Q>getTop())
            return memberQual;
        if (objectQual == null || objectQual == QualParams.<Q>getBottom()
                || objectQual == QualParams.<Q>getTop())
            return memberQual;
        return memberQual.substituteAll(objectQual);
    }


    private QualifierMapVisitor<QualParams<Q>, QualParams<Q>, QualParams<Q>> AS_MEMBER_OF_VISITOR =
        new QualifierMapVisitor<QualParams<Q>, QualParams<Q>, QualParams<Q>>() {
            @Override
            public QualParams<Q> process(QualParams<Q> memberQual, QualParams<Q> objectQual) {
                return qualifierAsMemberOf(memberQual, objectQual);
            }
        };

    @Override
    public QualifiedTypeMirror<QualParams<Q>> postAsMemberOf(
            QualifiedTypeMirror<QualParams<Q>> memberType,
            QualifiedTypeMirror<QualParams<Q>> receiverType,
            Element memberElement) {
        return AS_MEMBER_OF_VISITOR.visit(memberType, receiverType.getQualifier());
    }

    private QualifierMapVisitor<QualParams<Q>, QualParams<Q>, Map<String, Wildcard<Q>>> SUBSTITUTE_VISITOR =
        new QualifierMapVisitor<QualParams<Q>, QualParams<Q>, Map<String, Wildcard<Q>>>() {
            @Override
            public QualParams<Q> process(QualParams<Q> params, Map<String, Wildcard<Q>> substs) {
                return params.substituteAll(substs);
            }
        };

    /*
    @Override
    public Pair<QualifiedExecutableType<QualParams<Q>>, List<QualifiedTypeMirror<QualParams<Q>>>> methodFromUse(MethodInvocationTree tree) {
        Pair<QualifiedExecutableType<QualParams<Q>>, List<QualifiedTypeMirror<QualParams<Q>>>> result = super.methodFromUse(tree);

        List<? extends QualifiedTypeMirror<QualParams<Q>>> formals = result.first.getParameterTypes();
        List<QualifiedTypeMirror<QualParams<Q>>> actuals = new ArrayList<>();
        for (ExpressionTree actualExpr : tree.getArguments()) {
            actuals.add(getQualifiedType(actualExpr));
        }

        List<String> qualParams = new ArrayList<>();
        qualParams.add("Main");

        InferenceContext<Q> inference = new InferenceContext<>(qualParams, formals, actuals);
        QualifierParameterHierarchy<Q> hierarchy = (QualifierParameterHierarchy<Q>)getQualifierHierarchy();
        inference.run(getTypeHierarchy(), hierarchy);

        Map<String, Wildcard<Q>> subst = inference.getAssignment();

        if (subst != null) {
            QualifiedExecutableType<QualParams<Q>> newMethodType =
                (QualifiedExecutableType<QualParams<Q>>)SUBSTITUTE_VISITOR.visit(result.first, subst);
            List<QualifiedTypeMirror<QualParams<Q>>> newTypeArgs = new ArrayList<>();
            for (QualifiedTypeMirror<QualParams<Q>> qtm : result.second) {
                newTypeArgs.add(SUBSTITUTE_VISITOR.visit(qtm, subst));
            }
            result = Pair.of(newMethodType, newTypeArgs);
        } else {
            // TODO: report error
        }

        return result;
    }
    */


    public QualifiedTypeMirror<QualParams<Q>> postTypeVarSubstitution(QualifiedTypeVariable<QualParams<Q>> varDecl,
            QualifiedTypeVariable<QualParams<Q>> varUse, QualifiedTypeMirror<QualParams<Q>> value) {
        QualParams<Q> useParams = varUse.getQualifier();
        QualParams<Q> valueParams = value.getQualifier();

        QualifierParameterHierarchy<Q> hierarchy = (QualifierParameterHierarchy<Q>)getQualifierHierarchy();
        QualParams<Q> resultParams = hierarchy.leastUpperBound(useParams, valueParams);

        return value.accept(new SetQualifierVisitor<>(), resultParams);
    }
}
