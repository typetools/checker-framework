package org.checkerframework.framework.util.typeinference8;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;
import java.util.ArrayList;
import java.util.List;
import javax.lang.model.element.Element;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.typeinference8.types.AbstractType;
import org.checkerframework.javacutil.typeinference8.types.InvocationType;
import org.checkerframework.javacutil.typeinference8.types.Theta;

public class InvocationAnnotatedType implements InvocationType {
    private final ExpressionTree invocation;
    private final AnnotatedExecutableType methodType;
    private final CFInferenceContext context;
    AnnotatedTypeFactory typeFactory;

    InvocationAnnotatedType(
            AnnotatedExecutableType methodType,
            ExpressionTree invocation,
            CFInferenceContext context) {
        this.methodType = methodType;
        this.invocation = invocation;
        this.context = context;
    }

    @Override
    public ExecutableType getJavaType() {
        return methodType.getUnderlyingType();
    }

    @Override
    public List<? extends AbstractType> getThrownTypes(Theta map) {
        List<AbstractType> thrown = new ArrayList<>();
        for (AnnotatedTypeMirror t : methodType.getThrownTypes()) {
            thrown.add(InferenceAnnotatedType.create(t, map, context));
        }
        return thrown;
    }

    @Override
    public AbstractType getReturnType(Theta map) {
        AnnotatedTypeMirror returnType;
        if (invocation.getKind() == Tree.Kind.METHOD_INVOCATION
                || invocation.getKind() == Tree.Kind.MEMBER_REFERENCE) {
            returnType = methodType.getReturnType();
        } else if (TreeUtils.isDiamondTree(invocation)) {
            Element e = ElementUtils.enclosingClass(TreeUtils.elementFromUse(invocation));
            returnType = typeFactory.getAnnotatedType(e);
        } else {
            returnType = typeFactory.getAnnotatedType(invocation);
        }
        if (map == null) {
            return new ProperAnnotatedType(returnType, context);
        }
        return InferenceAnnotatedType.create(returnType, map, context);
    }

    /**
     * Returns a list of the parameter types of {@code InvocationType} where the vararg parameter
     * has been modified to match the arguments in {@code expression}.
     */
    @Override
    public List<AbstractType> getParameterTypes(Theta map, int size) {
        List<AnnotatedTypeMirror> params = new ArrayList<>(methodType.getParameterTypes());

        if (TreeUtils.isVarArgMethodCall(invocation)) {
            AnnotatedArrayType vararg = (AnnotatedArrayType) params.remove(params.size() - 1);
            for (int i = params.size(); i < size; i++) {
                params.add(vararg.getComponentType());
            }
        }
        return InferenceAnnotatedType.create(params, map, context);
    }

    @Override
    public boolean hasTypeVariables() {
        return !methodType.getTypeVariables().isEmpty();
    }

    public List<? extends AnnotatedTypeVariable> getTypeVariables() {
        return methodType.getTypeVariables();
    }

    @Override
    public boolean isVoid() {
        return methodType.getReturnType().getKind() == TypeKind.VOID;
    }

    @Override
    public List<AbstractType> getParameterTypes(Theta map) {
        return getParameterTypes(map, methodType.getParameterTypes().size());
    }
}
