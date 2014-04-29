package org.checkerframework.checker.qualparam;

import java.util.*;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;

import org.checkerframework.javacutil.Pair;

import org.checkerframework.qualframework.base.DefaultQualifiedTypeFactory;
import org.checkerframework.qualframework.base.QualifiedTypeMirror;
import org.checkerframework.qualframework.util.QualifierMapVisitor;
import org.checkerframework.qualframework.base.QualifiedTypeMirror.QualifiedExecutableType;

public abstract class QualifierParameterTypeFactory<Q> extends DefaultQualifiedTypeFactory<QualParams<Q>> {
    
    public Set<String> getDeclaredParameters(Element elt) {
        // TODO
        return new HashSet<>();
    }

    public final QualParams<Q> combineForDeclaration(QualParams<Q> curQual, QualParams<Q> newQual) {
        if (curQual == QualParams.<Q>getTop() || curQual == QualParams.<Q>getBottom()
                || newQual == QualParams.<Q>getTop() || newQual == QualParams.<Q>getBottom())
            throw new IllegalArgumentException(
                    "can't combine TOP and BOTTOM QualParams with others");

        Set<String> allKeys = new HashSet<>(curQual.keySet());
        allKeys.addAll(newQual.keySet());

        Map<String, ParamValue<Q>> combinedMap = new HashMap<>();
        for (String key : allKeys) {
            ParamValue<Q> resultValue =
                combineForTypeVarSubstitution(key, curQual.get(key), newQual.get(key));
            if (resultValue != null)
                combinedMap.put(key, resultValue);
        }
        return new QualParams<Q>(combinedMap);
    }

    public ParamValue<Q> combineForDeclaration(String paramName,
            ParamValue<Q> curValue, ParamValue<Q> newValue) {
        if (curValue == null)
            return newValue;
        else if (newValue == null)
            return curValue;
        else if (curValue.equals(newValue))
            return curValue;
        else
            // TODO: use normal error reporting mechanism.
            throw new IllegalArgumentException(
                    "tried to give two different values for qualparam " + paramName);
    }

    public final QualParams<Q> combineForArrayAccess(QualParams<Q> arrayQual, QualParams<Q> elementQual) {
        if (arrayQual == QualParams.<Q>getTop() || arrayQual == QualParams.<Q>getBottom()
                || elementQual == QualParams.<Q>getTop() || elementQual == QualParams.<Q>getBottom())
            throw new IllegalArgumentException(
                    "can't combine TOP and BOTTOM QualParams with others");

        Set<String> allKeys = new HashSet<>(arrayQual.keySet());
        allKeys.addAll(elementQual.keySet());

        Map<String, ParamValue<Q>> combinedMap = new HashMap<>();
        for (String key : allKeys) {
            ParamValue<Q> resultValue =
                combineForTypeVarSubstitution(key, arrayQual.get(key), elementQual.get(key));
            if (resultValue != null)
                combinedMap.put(key, resultValue);
        }
        return new QualParams<Q>(combinedMap);
    }

    public ParamValue<Q> combineForArrayAccess(String paramName,
            ParamValue<Q> arrayValue, ParamValue<Q> elementValue) {
        return elementValue;
    }

    public final QualParams<Q> combineForTypeVarSubstitution(QualParams<Q> useQual, QualParams<Q> actualQual) {
        if (useQual == QualParams.<Q>getTop() || useQual == QualParams.<Q>getBottom()
                || actualQual == QualParams.<Q>getTop() || actualQual == QualParams.<Q>getBottom())
            throw new IllegalArgumentException(
                    "can't combine TOP and BOTTOM QualParams with others");

        Set<String> allKeys = new HashSet<>(useQual.keySet());
        allKeys.addAll(actualQual.keySet());

        Map<String, ParamValue<Q>> combinedMap = new HashMap<>();
        for (String key : allKeys) {
            ParamValue<Q> resultValue =
                combineForTypeVarSubstitution(key, useQual.get(key), actualQual.get(key));
            if (resultValue != null)
                combinedMap.put(key, resultValue);
        }
        return new QualParams<Q>(combinedMap);
    }

    public ParamValue<Q> combineForTypeVarSubstitution(String paramName,
            ParamValue<Q> useValue, ParamValue<Q> actualValue) {
        if (useValue == null)
            return actualValue;
        else
            // TODO: use normal error reporting mechanism.
            throw new IllegalArgumentException(
                    "tried to apply qualifiers to a type variable use");
    }

    public final QualParams<Q> applyCaptureConversion(QualParams<Q> objectQual) {
        if (objectQual == null || objectQual == QualParams.<Q>getBottom()
                || objectQual == QualParams.<Q>getTop())
            return objectQual;
        return objectQual.capture();
    }

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

    private QualifierMapVisitor<QualParams<Q>, QualParams<Q>, Map<String, ParamValue<Q>>> SUBSTITUTE_VISITOR =
        new QualifierMapVisitor<QualParams<Q>, QualParams<Q>, Map<String, ParamValue<Q>>>() {
            @Override
            public QualParams<Q> process(QualParams<Q> params, Map<String, ParamValue<Q>> substs) {
                return params.substituteAll(substs);
            }
        };

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

        Map<String, ParamValue<Q>> subst = inference.getAssignment();

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
}
