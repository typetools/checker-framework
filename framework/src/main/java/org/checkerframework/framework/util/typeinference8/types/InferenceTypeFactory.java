package org.checkerframework.framework.util.typeinference8.types;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberReferenceTree;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.framework.util.typeinference8.util.InferenceUtils;
import org.checkerframework.framework.util.typeinference8.util.InternalInferenceUtils;
import org.checkerframework.framework.util.typeinference8.util.Java8InferenceContext;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;

public class InferenceTypeFactory {
    Java8InferenceContext context;

    public InferenceTypeFactory(Java8InferenceContext context) {
        this.context = context;
    }

    public InvocationType getTypeOfMethodAdaptedToUse(ExpressionTree invocation) {
        return new InvocationType(
                InternalInferenceUtils.getTypeOfMethodAdaptedToUse(invocation, context),
                invocation,
                context);
    }

    public ProperType getTargetType() {
        ProperType targetType = null;
        TypeMirror assignmentType = InferenceUtils.getTargetType(context.pathToExpression, context);

        if (assignmentType != null) {
            targetType = new ProperType(assignmentType, context);
        }
        return targetType;
    }

    public InvocationType compileTimeDeclarationType(MemberReferenceTree memRef) {
        return new InvocationType(
                TreeUtils.compileTimeDeclarationType(memRef, context.env), memRef, context);
    }

    public InvocationType findFunctionType(MemberReferenceTree memRef) {
        return new InvocationType(
                TypesUtils.findFunctionType(TreeUtils.typeOf(memRef), context.env),
                memRef,
                context);
    }
}
