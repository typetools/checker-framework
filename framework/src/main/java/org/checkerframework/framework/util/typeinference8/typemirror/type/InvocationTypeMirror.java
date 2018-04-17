package org.checkerframework.framework.util.typeinference8.typemirror.type;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;
import java.util.ArrayList;
import java.util.List;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import org.checkerframework.framework.util.typeinference8.types.AbstractType;
import org.checkerframework.framework.util.typeinference8.types.InvocationType;
import org.checkerframework.framework.util.typeinference8.types.Theta;
import org.checkerframework.framework.util.typeinference8.util.Java8InferenceContext;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;

public class InvocationTypeMirror implements InvocationType {
    private final ExpressionTree invocation;
    private final ExecutableType methodType;
    private final Java8InferenceContext context;

    InvocationTypeMirror(
            ExecutableType methodType, ExpressionTree invocation, Java8InferenceContext context) {
        this.methodType = methodType;
        this.invocation = invocation;
        this.context = context;
    }

    @Override
    public ExecutableType getJavaType() {
        return methodType;
    }

    @Override
    public List<? extends AbstractType> getThrownTypes(Theta map) {
        List<AbstractType> thrown = new ArrayList<>();
        for (TypeMirror t : methodType.getThrownTypes()) {
            thrown.add(InferenceTypeMirror.create(t, map, context));
        }
        return thrown;
    }

    @Override
    public AbstractType getReturnType(Theta map) {
        TypeMirror returnType;
        if (invocation.getKind() == Tree.Kind.METHOD_INVOCATION
                || invocation.getKind() == Tree.Kind.MEMBER_REFERENCE) {
            returnType = methodType.getReturnType();
        } else if (TreeUtils.isDiamondTree(invocation)) {
            returnType = ElementUtils.enclosingClass(TreeUtils.elementFromUse(invocation)).asType();
        } else {
            returnType = TreeUtils.typeOf(invocation);
        }
        if (map == null) {
            return new ProperTypeMirror(returnType, context);
        }
        return InferenceTypeMirror.create(returnType, map, context);
    }

    /**
     * Returns a list of the parameter types of {@code InvocationType} where the vararg parameter
     * has been modified to match the arguments in {@code expression}.
     */
    @Override
    public List<AbstractType> getParameterTypes(Theta map, int size) {
        List<TypeMirror> params = new ArrayList<>(methodType.getParameterTypes());

        if (TreeUtils.isVarArgMethodCall(invocation)) {
            ArrayType vararg = (ArrayType) params.remove(params.size() - 1);
            for (int i = params.size(); i < size; i++) {
                params.add(vararg.getComponentType());
            }
        }
        return InferenceTypeMirror.create(params, map, context);
    }

    @Override
    public List<? extends TypeVariable> getTypeVariables() {
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
