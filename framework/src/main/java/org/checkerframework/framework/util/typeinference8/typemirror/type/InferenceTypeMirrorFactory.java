package org.checkerframework.framework.util.typeinference8.typemirror.type;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import org.checkerframework.framework.util.typeinference8.constraint.Constraint.Kind;
import org.checkerframework.framework.util.typeinference8.constraint.ConstraintSet;
import org.checkerframework.framework.util.typeinference8.constraint.Typing;
import org.checkerframework.framework.util.typeinference8.types.AbstractType;
import org.checkerframework.framework.util.typeinference8.types.CaptureVariable;
import org.checkerframework.framework.util.typeinference8.types.ContainsInferenceVariable;
import org.checkerframework.framework.util.typeinference8.types.InferenceFactory;
import org.checkerframework.framework.util.typeinference8.types.InvocationType;
import org.checkerframework.framework.util.typeinference8.types.ProperType;
import org.checkerframework.framework.util.typeinference8.types.Theta;
import org.checkerframework.framework.util.typeinference8.types.Variable;
import org.checkerframework.framework.util.typeinference8.util.CheckedExceptionsUtil;
import org.checkerframework.framework.util.typeinference8.util.InferenceUtils;
import org.checkerframework.framework.util.typeinference8.util.InternalInferenceUtils;
import org.checkerframework.framework.util.typeinference8.util.Java8InferenceContext;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.ErrorReporter;
import org.checkerframework.javacutil.Pair;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;

public class InferenceTypeMirrorFactory implements InferenceFactory {
    private Java8InferenceContext context;

    public InferenceTypeMirrorFactory(Java8InferenceContext context) {
        this.context = context;
    }

    /**
     * If a mapping for {@code invocation} doesn't exist create it by:
     *
     * <p>Creates inference variables for the type parameters to {@code methodType} for a particular
     * {@code invocation}. Initializes the bounds of the variables. Returns a mapping from type
     * variables to newly created variables.
     *
     * <p>Otherwise, returns the previously created mapping.
     *
     * @param invocation method or constructor invocation
     * @param methodType type of generic method
     * @param context Java8InferenceContext
     * @return a mapping of the type variables of {@code methodType} to inference variables
     */
    @Override
    public Theta createTheta(
            ExpressionTree invocation, InvocationType methodType, Java8InferenceContext context) {
        if (context.maps.containsKey(invocation)) {
            return context.maps.get(invocation);
        }
        Theta map = new Theta();
        for (TypeVariable pl : methodType.getTypeVariables()) {
            Variable al = new VariableTypeMirror(pl, invocation, context);
            map.put(pl, al);
        }
        if (TreeUtils.isDiamondTree(invocation)) {
            DeclaredType classType =
                    (DeclaredType)
                            ElementUtils.enclosingClass(
                                            TreeUtils.elementFromUse((NewClassTree) invocation))
                                    .asType();
            for (TypeMirror typeMirror : classType.getTypeArguments()) {
                if (typeMirror.getKind() != TypeKind.TYPEVAR) {
                    ErrorReporter.errorAbort("Expected type variable, found: %s", typeMirror);
                    return map;
                }
                TypeVariable pl = (TypeVariable) typeMirror;
                Variable al = new VariableTypeMirror(pl, invocation, context);
                map.put(pl, al);
            }
        }

        for (Variable v : map.values()) {
            v.initialBounds(map);
        }
        context.maps.put(invocation, map);
        return map;
    }

    @Override
    public Theta createTheta(LambdaExpressionTree lambda, AbstractType t) {
        TypeElement typeEle = (TypeElement) ((DeclaredType) t.getJavaType()).asElement();

        Theta map = new Theta();
        for (TypeParameterElement param : typeEle.getTypeParameters()) {
            TypeVariable typeVar = (TypeVariable) param.asType();
            Variable ai = new VariableTypeMirror(typeVar, lambda, context);
            map.put(typeVar, ai);
        }
        return map;
    }

    @Override
    public Theta createThetaForCapture(ExpressionTree tree, AbstractType capturedType) {
        DeclaredType underlying = (DeclaredType) capturedType.getJavaType();
        TypeElement ele = TypesUtils.getTypeElement(underlying);
        Theta map = new Theta();
        for (TypeParameterElement pEle : ele.getTypeParameters()) {
            TypeVariable pl = (TypeVariable) pEle.asType();
            CaptureVariable al = new CaptureVariableTypeMirror(pl, tree, context);
            map.put(pl, al);
        }
        return map;
    }

    @Override
    public InvocationType getTypeOfMethodAdaptedToUse(ExpressionTree invocation) {
        return new InvocationTypeMirror(
                InternalInferenceUtils.getTypeOfMethodAdaptedToUse(invocation, context),
                invocation,
                context);
    }

    @Override
    public ProperType getTargetType() {
        ProperType targetType = null;
        TypeMirror assignmentType = InferenceUtils.getTargetType(context.pathToExpression, context);

        if (assignmentType != null) {
            targetType = new ProperTypeMirror(assignmentType, context);
        }
        return targetType;
    }

    @Override
    public InvocationType compileTimeDeclarationType(MemberReferenceTree memRef) {
        return new InvocationTypeMirror(
                TreeUtils.compileTimeDeclarationType(memRef, context.env), memRef, context);
    }

    @Override
    public InvocationType findFunctionType(MemberReferenceTree memRef) {
        return new InvocationTypeMirror(
                TypesUtils.findFunctionType(TreeUtils.typeOf(memRef), context.env),
                memRef,
                context);
    }

    @Override
    public List<AbstractType> findParametersOfFunctionType(AbstractType t, Theta map) {
        TypeElement typeEle = (TypeElement) ((DeclaredType) t.getJavaType()).asElement();
        ExecutableType funcType = TypesUtils.findFunctionType(typeEle.asType(), context.env);
        List<AbstractType> qs = new ArrayList<>();
        for (TypeMirror param : funcType.getParameterTypes()) {
            qs.add(InferenceTypeMirror.create(param, map, context));
        }
        return qs;
    }

    @Override
    public Pair<AbstractType, AbstractType> getParameterizedSupers(AbstractType a, AbstractType b) {
        TypeMirror aTypeMirror = a.getJavaType();
        TypeMirror bTypeMirror = b.getJavaType();
        // com.sun.tools.javac.comp.Infer#getParameterizedSupers
        TypeMirror lubResult = InternalInferenceUtils.lub(context.env, aTypeMirror, bTypeMirror);
        if (!TypesUtils.isParameterizedType(lubResult)) {
            return null;
        }

        Type asSuperOfA = context.types.asSuper((Type) aTypeMirror, ((Type) lubResult).asElement());
        Type asSuperOfB = context.types.asSuper((Type) bTypeMirror, ((Type) lubResult).asElement());

        AbstractType superA = ((AbstractTypeMirror) a).create(asSuperOfA);
        AbstractType superB = ((AbstractTypeMirror) b).create(asSuperOfB);
        return Pair.of(a.asSuper(superA), b.asSuper(superB));
    }

    @Override
    public ProperType getTypeOfExpression(ExpressionTree tree) {
        return new ProperTypeMirror(tree, context);
    }

    @Override
    public ProperType getTypeOfVariable(VariableTree tree) {
        return new ProperTypeMirror(tree, context);
    }

    @Override
    public AbstractType getTypeOfElement(Element element, Theta map) {
        return InferenceTypeMirror.create(element.asType(), map, context);
    }

    @Override
    public ProperType getObject() {
        TypeMirror objecTypeMirror =
                TypesUtils.typeFromClass(
                        Object.class, context.modelTypes, context.env.getElementUtils());
        return new ProperTypeMirror(objecTypeMirror, context);
    }

    @Override
    public ProperType lub(LinkedHashSet<ProperType> lowerBounds) {
        if (lowerBounds.isEmpty()) {
            return null;
        }
        TypeMirror ti = null;
        for (ProperType liProperType : lowerBounds) {
            TypeMirror li = liProperType.getJavaType();
            if (ti == null) {
                ti = li;
            } else {
                ti = InternalInferenceUtils.lub(context.env, ti, li);
            }
        }
        return new ProperTypeMirror(ti, context);
    }

    @Override
    public AbstractType glb(LinkedHashSet<AbstractType> lowerBounds) {
        AbstractType ti = null;
        for (AbstractType liProperType : lowerBounds) {
            AbstractType li = liProperType;
            if (ti == null) {
                ti = li;
            } else {
                ti = glb(ti, li);
            }
        }
        return ti;
    }

    @Override
    public AbstractType glb(AbstractType a, AbstractType b) {
        Type aJavaType = (Type) a.getJavaType();
        Type bJavaType = (Type) b.getJavaType();
        TypeMirror glb = TypesUtils.greatestLowerBound(aJavaType, bJavaType, context.env);
        if (context.env.getTypeUtils().isSameType(glb, bJavaType)) {
            return b;
        } else if (context.env.getTypeUtils().isSameType(glb, aJavaType)) {
            return a;
        } else if (a.isInferenceType()) {
            return ((AbstractTypeMirror) a).create(glb);
        } else if (b.isInferenceType()) {
            return ((AbstractTypeMirror) b).create(glb);
        }
        assert a.isProper() && b.isProper();
        return new ProperTypeMirror(glb, context);
    }

    @Override
    public ProperType getRuntimeException() {
        return new ProperTypeMirror(context.runtimeEx, context);
    }

    @Override
    public ConstraintSet getCheckedExceptionConstraints(ExpressionTree expression, Theta map) {
        ConstraintSet constraintSet = new ConstraintSet();
        ExecutableElement ele = (ExecutableElement) TreeUtils.findFunction(expression, context.env);
        List<Variable> es = new ArrayList<>();
        List<ProperType> properTypes = new ArrayList<>();
        for (TypeMirror thrownType : ele.getThrownTypes()) {
            AbstractType ei = InferenceTypeMirror.create(thrownType, map, context);
            if (ei.isProper()) {
                properTypes.add((ProperType) ei);
            } else {
                es.add((Variable) ei);
            }
        }
        if (es.isEmpty()) {
            return ConstraintSet.TRUE;
        }

        List<? extends TypeMirror> thrownTypes;
        if (expression.getKind() == Tree.Kind.LAMBDA_EXPRESSION) {
            thrownTypes =
                    CheckedExceptionsUtil.thrownCheckedExceptions(
                            (LambdaExpressionTree) expression, context);
        } else {
            thrownTypes =
                    TypesUtils.findFunctionType(TreeUtils.typeOf(expression), context.env)
                            .getThrownTypes();
        }

        for (TypeMirror xi : thrownTypes) {
            boolean isSubtypeOfProper = false;
            for (ProperType properType : properTypes) {
                if (context.env.getTypeUtils().isSubtype(xi, properType.getJavaType())) {
                    isSubtypeOfProper = true;
                }
            }
            if (!isSubtypeOfProper) {
                for (Variable ei : es) {
                    constraintSet.add(
                            new Typing(new ProperTypeMirror(xi, context), ei, Kind.SUBTYPE));
                    ei.getBounds().setHasThrowsBound(true);
                }
            }
        }

        return constraintSet;
    }

    @Override
    public ProperType createWildcard(ProperType lowerBound, AbstractType upperBound) {
        TypeMirror wildcard =
                TypesUtils.createWildcard(
                        lowerBound == null ? null : lowerBound.getJavaType(),
                        upperBound == null ? null : upperBound.getJavaType(),
                        context.env.getTypeUtils());
        return new ProperTypeMirror(wildcard, context);
    }

    @Override
    public List<ProperType> getSubsTypeArgs(
            List<TypeVariable> typeVar, List<ProperType> typeArg, List<Variable> asList) {
        List<TypeMirror> javaTypeArgs = new ArrayList<>();
        // Recursive types:
        for (int i = 0; i < typeArg.size(); i++) {
            Variable ai = asList.get(i);
            TypeMirror inst = typeArg.get(i).getJavaType();
            TypeVariable typeVariableI = ai.getJavaType();
            if (ContainsInferenceVariable.hasAnyTypeVariable(
                    Collections.singleton(typeVariableI), inst)) {
                // If the instantiation of ai includes a reference to ai,
                // then substitute ai with an unbound wildcard.  This isn't quite right but I'm not
                // sure how to make recursive types Java types.
                // TODO: This causes problems when incorporating the bounds.
                TypeMirror unbound = context.env.getTypeUtils().getWildcardType(null, null);
                inst =
                        TypesUtils.substitute(
                                inst,
                                Collections.singletonList(typeVariableI),
                                Collections.singletonList(unbound),
                                context.env);
                javaTypeArgs.add(inst);
            } else {
                javaTypeArgs.add(inst);
            }
        }

        // Instantiations that refer to another variable
        List<ProperType> subsTypeArg = new ArrayList<>();
        for (TypeMirror type : javaTypeArgs) {
            TypeMirror subs = TypesUtils.substitute(type, typeVar, javaTypeArgs, context.env);
            subsTypeArg.add(new ProperTypeMirror(subs, context));
        }
        return subsTypeArg;
    }
}
