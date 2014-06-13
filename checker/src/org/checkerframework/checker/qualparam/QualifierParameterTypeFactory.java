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
import org.checkerframework.qualframework.base.QualifierMapVisitor;
import org.checkerframework.qualframework.base.SetQualifierVisitor;
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

    @Override
    public Pair<QualifiedExecutableType<QualParams<Q>>, List<QualifiedTypeMirror<QualParams<Q>>>> methodFromUse(MethodInvocationTree tree) {
        Pair<QualifiedExecutableType<QualParams<Q>>, List<QualifiedTypeMirror<QualParams<Q>>>> result = super.methodFromUse(tree);

        List<? extends QualifiedTypeMirror<QualParams<Q>>> formals =
            getQualifiedTypes().expandVarArgs(result.first, tree.getArguments());
        List<QualifiedTypeMirror<QualParams<Q>>> actuals = new ArrayList<>();
        for (ExpressionTree actualExpr : tree.getArguments()) {
            actuals.add(getQualifiedType(actualExpr));
        }

        // TODO: look at the actual declared params of the method
        List<String> qualParams = new ArrayList<>();
        qualParams.add("Main");

        QualifierParameterHierarchy<Q> hierarchy = (QualifierParameterHierarchy<Q>)getQualifierHierarchy();
        InferenceContext<Q> inference = new InferenceContext<>(qualParams, formals, actuals,
                groundHierarchy, new PolyQualHierarchy<>(groundHierarchy));
        inference.run(getTypeHierarchy(), hierarchy);

        Map<String, PolyQual<Q>> subst = inference.getAssignment();

        if (subst != null) {
            Map<String, Wildcard<Q>> wildSubst = new HashMap<>();
            for (String name : subst.keySet()) {
                wildSubst.put(name, new Wildcard<>(subst.get(name)));
            }

            QualifiedExecutableType<QualParams<Q>> newMethodType =
                (QualifiedExecutableType<QualParams<Q>>)SUBSTITUTE_VISITOR.visit(result.first, wildSubst);
            List<QualifiedTypeMirror<QualParams<Q>>> newTypeArgs = new ArrayList<>();
            for (QualifiedTypeMirror<QualParams<Q>> qtm : result.second) {
                newTypeArgs.add(SUBSTITUTE_VISITOR.visit(qtm, wildSubst));
            }
            result = Pair.of(newMethodType, newTypeArgs);
        } else {
            // TODO: report error
        }

        return result;
    }


    protected abstract Wildcard<Q> combineForSubstitution(Wildcard<Q> a, Wildcard<Q> b);

    public QualifiedTypeMirror<QualParams<Q>> postTypeVarSubstitution(QualifiedTypeVariable<QualParams<Q>> varDecl,
            QualifiedTypeVariable<QualParams<Q>> varUse, QualifiedTypeMirror<QualParams<Q>> value) {
        QualParams<Q> useParams = varUse.getQualifier();
        QualParams<Q> valueParams = value.getQualifier();

        HashMap<String, Wildcard<Q>> newParams = new HashMap<>(useParams);
        for (String name : valueParams.keySet()) {
            Wildcard<Q> newValue = valueParams.get(name);

            Wildcard<Q> oldValue = newParams.get(name);
            if (oldValue != null) {
                newValue = combineForSubstitution(oldValue, newValue);
            }

            newParams.put(name, newValue);
        }

        return value.accept(new SetQualifierVisitor<>(), new QualParams<>(newParams));
    }
}
