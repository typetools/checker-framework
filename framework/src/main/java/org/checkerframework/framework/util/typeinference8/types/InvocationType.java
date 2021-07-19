package org.checkerframework.framework.util.typeinference8.types;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.lang.model.element.Element;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.util.typeinference8.util.Java8InferenceContext;
import org.checkerframework.framework.util.typeinference8.util.Theta;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;

public class InvocationType {

    private final ExpressionTree invocation;
    private final AnnotatedExecutableType annotatedExecutableType;
    private final ExecutableType methodType;
    private final Java8InferenceContext context;
    private final AnnotatedTypeFactory typeFactory;

    public InvocationType(
            AnnotatedExecutableType annotatedExecutableType,
            ExecutableType methodType,
            ExpressionTree invocation,
            Java8InferenceContext context) {
        assert annotatedExecutableType != null && methodType != null;
        this.annotatedExecutableType = annotatedExecutableType;
        this.methodType = methodType;
        this.invocation = invocation;
        this.context = context;
        this.typeFactory = context.typeFactory;
    }

    public ExecutableType getJavaType() {
        return annotatedExecutableType.getUnderlyingType();
    }

    public List<? extends AbstractType> getThrownTypes(Theta map) {
        List<AbstractType> thrown = new ArrayList<>();
        Iterator<? extends TypeMirror> iter = methodType.getThrownTypes().iterator();
        for (AnnotatedTypeMirror t : annotatedExecutableType.getThrownTypes()) {
            thrown.add(InferenceType.create(t, iter.next(), map, context));
        }
        return thrown;
    }

    public AbstractType getReturnType(Theta map) {
        TypeMirror returnTypeJava;
        AnnotatedTypeMirror returnType;

        if (TreeUtils.isDiamondTree(invocation) || TreeUtils.isDiamondMemberReference(invocation)) {
            Element e = ElementUtils.enclosingTypeElement(TreeUtils.elementFromUse(invocation));
            returnTypeJava = e.asType();
            returnType = typeFactory.getAnnotatedType(e);
        } else if (invocation.getKind() == Tree.Kind.METHOD_INVOCATION
                || invocation.getKind() == Tree.Kind.MEMBER_REFERENCE) {
            returnTypeJava = methodType.getReturnType();
            returnType = annotatedExecutableType.getReturnType();
        } else {
            returnTypeJava = TreeUtils.typeOf(invocation);
            returnType = typeFactory.getAnnotatedType(invocation);
        }

        if (map == null) {
            return new ProperType(returnType, returnTypeJava, context);
        }
        return InferenceType.create(returnType, returnTypeJava, map, context);
    }

    /**
     * Returns a list of the parameter types of {@code InvocationType} where the vararg parameter
     * has been modified to match the arguments in {@code expression}.
     */
    public List<AbstractType> getParameterTypes(Theta map, int size) {
        List<AnnotatedTypeMirror> params =
                new ArrayList<>(annotatedExecutableType.getParameterTypes());

        if (TreeUtils.isVarArgMethodCall(invocation)) {
            AnnotatedArrayType vararg = (AnnotatedArrayType) params.remove(params.size() - 1);
            for (int i = params.size(); i < size; i++) {
                params.add(vararg.getComponentType());
            }
        }

        List<TypeMirror> paramsJava = new ArrayList<>(methodType.getParameterTypes());

        if (TreeUtils.isVarArgMethodCall(invocation)) {
            ArrayType vararg = (ArrayType) paramsJava.remove(paramsJava.size() - 1);
            for (int i = paramsJava.size(); i < size; i++) {
                paramsJava.add(vararg.getComponentType());
            }
        }
        return InferenceType.create(params, paramsJava, map, context);
    }

    public boolean hasTypeVariables() {
        return !annotatedExecutableType.getTypeVariables().isEmpty();
    }

    public List<? extends AnnotatedTypeVariable> getAnnotatedTypeVariables() {
        return annotatedExecutableType.getTypeVariables();
    }

    public List<? extends TypeVariable> getTypeVariables() {
        return methodType.getTypeVariables();
    }

    public boolean isVoid() {
        return annotatedExecutableType.getReturnType().getKind() == TypeKind.VOID;
    }

    public List<AbstractType> getParameterTypes(Theta map) {
        return getParameterTypes(map, annotatedExecutableType.getParameterTypes().size());
    }

    public AnnotatedExecutableType getAnnotatedType() {
        return annotatedExecutableType;
    }
}
