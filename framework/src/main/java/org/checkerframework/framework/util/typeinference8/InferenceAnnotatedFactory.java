package org.checkerframework.framework.util.typeinference8;

import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree.JCNewClass;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.framework.util.typeinference.TypeArgInferenceUtil;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.ErrorReporter;
import org.checkerframework.javacutil.Pair;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypeAnnotationUtils;
import org.checkerframework.javacutil.TypesUtils;
import org.checkerframework.javacutil.typeinference8.constraint.Constraint;
import org.checkerframework.javacutil.typeinference8.constraint.ConstraintSet;
import org.checkerframework.javacutil.typeinference8.constraint.Typing;
import org.checkerframework.javacutil.typeinference8.typemirror.type.AbstractTypeMirror;
import org.checkerframework.javacutil.typeinference8.types.AbstractType;
import org.checkerframework.javacutil.typeinference8.types.CaptureVariable;
import org.checkerframework.javacutil.typeinference8.types.InferenceFactory;
import org.checkerframework.javacutil.typeinference8.types.InvocationType;
import org.checkerframework.javacutil.typeinference8.types.ProperType;
import org.checkerframework.javacutil.typeinference8.types.Theta;
import org.checkerframework.javacutil.typeinference8.types.Variable;
import org.checkerframework.javacutil.typeinference8.util.Java8InferenceContext;

public class InferenceAnnotatedFactory implements InferenceFactory {
    private final CFInferenceContext context;
    private final AnnotatedTypeFactory typeFactory;

    public InferenceAnnotatedFactory(CFInferenceContext context) {
        this.context = context;
        typeFactory = context.typeFactory;
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
        InvocationAnnotatedType annotatedMethodType = (InvocationAnnotatedType) methodType;
        Theta map = new Theta();
        for (AnnotatedTypeVariable pl : annotatedMethodType.getTypeVariables()) {
            Variable al = new VariableAnnotatedType(pl, invocation, (CFInferenceContext) context);
            map.put(pl.getUnderlyingType(), al);
        }
        if (TreeUtils.isDiamondTree(invocation)) {
            Element classEle =
                    ElementUtils.enclosingClass(
                            TreeUtils.elementFromUse((NewClassTree) invocation));
            AnnotatedDeclaredType classType =
                    (AnnotatedDeclaredType) typeFactory.getAnnotatedType(classEle);

            for (AnnotatedTypeMirror typeMirror : classType.getTypeArguments()) {
                if (typeMirror.getKind() != TypeKind.TYPEVAR) {
                    ErrorReporter.errorAbort("Expected type variable, found: %s", typeMirror);
                    return map;
                }
                AnnotatedTypeVariable pl = (AnnotatedTypeVariable) typeMirror;
                Variable al =
                        new VariableAnnotatedType(pl, invocation, (CFInferenceContext) context);
                map.put(pl.getUnderlyingType(), al);
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
        AnnotatedDeclaredType classType = typeFactory.getAnnotatedType(typeEle);

        Theta map = new Theta();
        for (AnnotatedTypeMirror param : classType.getTypeArguments()) {
            AnnotatedTypeVariable typeVar = (AnnotatedTypeVariable) param;
            Variable ai = new VariableAnnotatedType(typeVar, lambda, context);
            map.put(typeVar.getUnderlyingType(), ai);
        }
        return map;
    }

    @Override
    public Theta createThetaForCapture(ExpressionTree tree, AbstractType capturedType) {
        DeclaredType underlying = (DeclaredType) capturedType.getJavaType();
        TypeElement ele = TypesUtils.getTypeElement(underlying);
        AnnotatedDeclaredType classType = typeFactory.getAnnotatedType(ele);

        Theta map = new Theta();
        for (AnnotatedTypeMirror pEle : classType.getTypeArguments()) {
            AnnotatedTypeVariable pl = (AnnotatedTypeVariable) pEle;
            CaptureVariable al = new CaptureVariableAnnotatedType(pl, tree, context);
            map.put(pl.getUnderlyingType(), al);
        }
        return map;
    }

    @Override
    public InvocationType getTypeOfMethodAdaptedToUse(ExpressionTree invocation) {
        return new InvocationAnnotatedType(
                (AnnotatedExecutableType) typeFactory.getAnnotatedType(invocation),
                invocation,
                context);
    }
    /**
     * Returns the type that the leaf of path is assigned to, if it is within an assignment context.
     * Returns the type that the method invocation at the leaf is assigned to. If the result is a
     * primitive, return the boxed version.
     *
     * @return type that path leaf is assigned to
     */
    public static TypeMirror getTargetType(TreePath path, Java8InferenceContext context) {
        Tree assignmentContext = TreeUtils.getAssignmentContext(path);
        if (assignmentContext == null) {
            return null;
        }

        switch (assignmentContext.getKind()) {
            case ASSIGNMENT:
                ExpressionTree variable = ((AssignmentTree) assignmentContext).getVariable();
                return TreeUtils.typeOf(variable);
            case VARIABLE:
                VariableTree variableTree = (VariableTree) assignmentContext;
                return TreeUtils.typeOf(variableTree.getType());
            case METHOD_INVOCATION:
                MethodInvocationTree methodInvocation = (MethodInvocationTree) assignmentContext;
                return assignedToExecutable(
                        path, methodInvocation, methodInvocation.getArguments(), context);
            case NEW_CLASS:
                NewClassTree newClassTree = (NewClassTree) assignmentContext;
                return assignedToExecutable(
                        path, newClassTree, newClassTree.getArguments(), context);
            case NEW_ARRAY:
                NewArrayTree newArrayTree = (NewArrayTree) assignmentContext;
                ArrayType arrayType = (ArrayType) TreeUtils.typeOf(newArrayTree);
                return arrayType.getComponentType();
            case RETURN:
                HashSet<Kind> kinds =
                        new HashSet<>(Arrays.asList(Tree.Kind.LAMBDA_EXPRESSION, Tree.Kind.METHOD));
                Tree enclosing = TreeUtils.enclosingOfKind(path, kinds);
                if (enclosing.getKind() == Tree.Kind.METHOD) {
                    MethodTree methodTree = (MethodTree) enclosing;
                    return TreeUtils.typeOf(methodTree.getReturnType());
                } else {
                    // TODO: I don't think this should happen. during inference
                    LambdaExpressionTree lambdaTree = (LambdaExpressionTree) enclosing;
                    return TreeUtils.typeOf(lambdaTree);
                }
            default:
                if (assignmentContext
                        .getKind()
                        .asInterface()
                        .equals(CompoundAssignmentTree.class)) {
                    // 11 Tree kinds are compound assignments, so don't use it in the switch
                    ExpressionTree var = ((CompoundAssignmentTree) assignmentContext).getVariable();
                    return TreeUtils.typeOf(var);
                } else {
                    ErrorReporter.errorAbort(
                            "Unexpected assignment context.\nKind: %s\nTree: %s",
                            assignmentContext.getKind(), assignmentContext);
                    return null;
                }
        }
    }

    private static TypeMirror assignedToExecutable(
            TreePath path,
            ExpressionTree methodInvocation,
            List<? extends ExpressionTree> arguments,
            Java8InferenceContext context) {
        int treeIndex = -1;
        for (int i = 0; i < arguments.size(); ++i) {
            ExpressionTree argumentTree = arguments.get(i);
            if (isArgument(path, argumentTree)) {
                treeIndex = i;
                break;
            }
        }

        ExecutableType methodType = getTypeOfMethodAdaptedToUse(methodInvocation, context);
        if (treeIndex >= methodType.getParameterTypes().size() - 1
                && TreeUtils.isVarArgMethodCall(methodInvocation)) {
            treeIndex = methodType.getParameterTypes().size() - 1;
            TypeMirror typeMirror = methodType.getParameterTypes().get(treeIndex);
            return ((ArrayType) typeMirror).getComponentType();
        }

        return methodType.getParameterTypes().get(treeIndex);
    }

    /**
     * Returns whether argumentTree is the tree at the leaf of path. if tree is a conditional
     * expression, isArgument is called recursively on the true and false expressions.
     */
    private static boolean isArgument(TreePath path, ExpressionTree argumentTree) {
        argumentTree = TreeUtils.skipParens(argumentTree);
        if (argumentTree == path.getLeaf()) {
            return true;
        } else if (argumentTree.getKind() == Tree.Kind.CONDITIONAL_EXPRESSION) {
            ConditionalExpressionTree conditionalExpressionTree =
                    (ConditionalExpressionTree) argumentTree;
            return isArgument(path, conditionalExpressionTree.getTrueExpression())
                    || isArgument(path, conditionalExpressionTree.getFalseExpression());
        }
        return false;
    }

    private static DeclaredType getReceiverType(ExpressionTree tree) {
        Tree receiverTree;
        if (tree.getKind() == Tree.Kind.NEW_CLASS) {
            receiverTree = ((NewClassTree) tree).getEnclosingExpression();
        } else {
            receiverTree = TreeUtils.getReceiverTree(tree);
        }

        if (receiverTree == null) {
            return null;
        }
        TypeMirror type = TreeUtils.typeOf(receiverTree);
        if (type.getKind() == TypeKind.TYPEVAR) {
            return (DeclaredType) ((TypeVariable) type).getUpperBound();
        }
        return type.getKind() == TypeKind.DECLARED ? (DeclaredType) type : null;
    }

    /**
     * @return ExecutableType of the method invocation or new class tree adapted to the call site.
     */
    public static ExecutableType getTypeOfMethodAdaptedToUse(
            ExpressionTree expressionTree, Java8InferenceContext context) {
        if (expressionTree.getKind() == Tree.Kind.NEW_CLASS) {
            if (!TreeUtils.isDiamondTree(expressionTree)) {
                return (ExecutableType) ((JCNewClass) expressionTree).constructorType;
            }
        } else if (expressionTree.getKind() != Tree.Kind.METHOD_INVOCATION) {
            return null;
        }
        ExecutableElement ele = (ExecutableElement) TreeUtils.elementFromUse(expressionTree);

        if (ElementUtils.isStatic(ele)) {
            return (ExecutableType) ele.asType();
        }
        DeclaredType receiverType = getReceiverType(expressionTree);

        if (receiverType == null) {
            receiverType = context.enclosingType;
        }

        while (context.types.asSuper((Type) receiverType, (Symbol) ele.getEnclosingElement())
                == null) {
            TypeMirror enclosing = receiverType.getEnclosingType();
            if (enclosing == null || enclosing.getKind() != TypeKind.DECLARED) {
                if (expressionTree.getKind() == Tree.Kind.NEW_CLASS) {
                    // No receiver for the constructor.
                    return (ExecutableType) ele.asType();
                } else {
                    ErrorReporter.errorAbort("Method not found");
                }
            }
            receiverType = (DeclaredType) enclosing;
        }
        javax.lang.model.util.Types types = context.env.getTypeUtils();
        return (ExecutableType) types.asMemberOf(receiverType, ele);
    }

    @Override
    public ProperType getTargetType() {
        ProperType targetType = null;
        AnnotatedTypeMirror assignmentType =
                TypeArgInferenceUtil.assignedTo(typeFactory, context.pathToExpression);

        if (assignmentType != null) {
            targetType = new ProperAnnotatedType(assignmentType, context);
        }
        return targetType;
    }

    @Override
    public InvocationType compileTimeDeclarationType(MemberReferenceTree memRef) {
        Pair<AnnotatedDeclaredType, AnnotatedExecutableType> result =
                typeFactory.getFnInterfaceFromTree(memRef);
        // The type of the single method that is declared by the functional interface.
        AnnotatedExecutableType functionType = result.second;

        AnnotatedTypeMirror enclosingType =
                typeFactory.getEnclosingTypeOfMemberReference(memRef, functionType);
        AnnotatedExecutableType compileTimeType =
                typeFactory.getCompileTimeDeclarationMemberReference(
                        memRef, functionType, enclosingType);
        return new InvocationAnnotatedType(compileTimeType, memRef, context);
    }

    @Override
    public InvocationType findFunctionType(MemberReferenceTree memRef) {
        Pair<AnnotatedDeclaredType, AnnotatedExecutableType> result =
                typeFactory.getFnInterfaceFromTree(memRef);
        // The type of the single method that is declared by the functional interface.
        AnnotatedExecutableType functionType = result.second;

        return new InvocationAnnotatedType(functionType, memRef, context);
    }

    @Override
    public List<AbstractType> findParametersOfFunctionType(AbstractType t, Theta map) {

        TypeElement typeEle = (TypeElement) ((DeclaredType) t.getJavaType()).asElement();
        AnnotatedExecutableType funcType =
                typeFactory.getFunctionType(
                        typeEle,
                        (AnnotatedDeclaredType) ((AbstractAnnotatedType) t).getAnnotatedType());
        List<AbstractType> qs = new ArrayList<>();
        for (AnnotatedTypeMirror param : funcType.getParameterTypes()) {
            qs.add(InferenceAnnotatedType.create(param, map, context));
        }
        return qs;
    }

    @Override
    public Pair<AbstractType, AbstractType> getParameterizedSupers(AbstractType a, AbstractType b) {
        TypeMirror aTypeMirror = a.getJavaType();
        TypeMirror bTypeMirror = b.getJavaType();
        // com.sun.tools.javac.comp.Infer#getParameterizedSupers
        TypeMirror lubResult = lub(context.env, aTypeMirror, bTypeMirror);
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
        return new ProperAnnotatedType(typeFactory.getAnnotatedType(tree), context);
    }

    @Override
    public ProperType getTypeOfVariable(VariableTree tree) {
        return new ProperAnnotatedType(typeFactory.getAnnotatedType(tree), context);
    }

    @Override
    public AbstractType getTypeOfElement(Element element, Theta map) {
        AnnotatedTypeMirror atm = typeFactory.getAnnotatedType(element);
        return InferenceAnnotatedType.create(atm, map, context);
    }

    @Override
    public AbstractType getTypeOfBound(TypeParameterElement pEle, Theta map) {
        AnnotatedTypeVariable atm = (AnnotatedTypeVariable) typeFactory.getAnnotatedType(pEle);

        return InferenceAnnotatedType.create(atm.getUpperBound(), map, context);
    }

    @Override
    public ProperType getObject() {
        throw new RuntimeException("Not implemented");
        //        TypeMirror objecTypeMirror =
        //                TypesUtils.typeFromClass(
        //                        Object.class, context.modelTypes, context.env.getElementUtils());
        //        return new ProperTypeMirror(objecTypeMirror, context);
    }

    @Override
    public ProperType lub(LinkedHashSet<ProperType> lowerBounds) {
        if (lowerBounds.isEmpty()) {
            return null;
        }
        AnnotatedTypeMirror ti = null;
        for (ProperType liProperType : lowerBounds) {
            AnnotatedTypeMirror li = ((ProperAnnotatedType) liProperType).getAnnotatedType();
            if (ti == null) {
                ti = li;
            } else {
                ti = AnnotatedTypes.leastUpperBound(typeFactory, ti, li);
            }
        }
        return new ProperAnnotatedType(ti, context);
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
                if (ti == null) {
                    return null;
                }
            }
        }
        return ti;
    }

    @Override
    public AbstractType glb(AbstractType a, AbstractType b) {
        AnnotatedTypeMirror aAtm = ((AbstractAnnotatedType) a).getAnnotatedType();
        AnnotatedTypeMirror bAtm = ((AbstractAnnotatedType) b).getAnnotatedType();
        AnnotatedTypeMirror glb = AnnotatedTypes.greatestLowerBound(typeFactory, aAtm, bAtm);
        if (a.isInferenceType()) {
            return ((AbstractAnnotatedType) a).create(glb);
        } else if (b.isInferenceType()) {
            return ((AbstractAnnotatedType) b).create(glb);
        }

        assert a.isProper() && b.isProper();
        return new ProperAnnotatedType(glb, context);
    }

    @Override
    public ProperType getRuntimeException() {
        throw new RuntimeException("Not implemented");
        //        return new ProperTypeMirror(context.runtimeEx, context);
    }

    @Override
    public ConstraintSet getCheckedExceptionConstraints(ExpressionTree expression, Theta map) {
        ConstraintSet constraintSet = new ConstraintSet();
        Pair<AnnotatedDeclaredType, AnnotatedExecutableType> pair;
        if (expression.getKind() == Kind.LAMBDA_EXPRESSION) {
            pair = typeFactory.getFnInterfaceFromTree((LambdaExpressionTree) expression);
        } else {
            pair = typeFactory.getFnInterfaceFromTree((MemberReferenceTree) expression);
        }
        List<Variable> es = new ArrayList<>();
        List<ProperAnnotatedType> properTypes = new ArrayList<>();
        for (AnnotatedTypeMirror thrownType : pair.second.getThrownTypes()) {
            AbstractType ei = InferenceAnnotatedType.create(thrownType, map, context);
            if (ei.isProper()) {
                properTypes.add((ProperAnnotatedType) ei);
            } else {
                es.add((Variable) ei);
            }
        }
        if (es.isEmpty()) {
            return ConstraintSet.TRUE;
        }

        List<? extends AnnotatedTypeMirror> thrownTypes;
        if (expression.getKind() == Tree.Kind.LAMBDA_EXPRESSION) {
            thrownTypes =
                    CheckedExceptionsUtil.thrownCheckedExceptions(
                            (LambdaExpressionTree) expression, context);
        } else {
            AnnotatedTypeMirror enclosing =
                    typeFactory.getEnclosingTypeOfMemberReference(
                            (MemberReferenceTree) expression, pair.second);
            thrownTypes =
                    typeFactory
                            .getCompileTimeDeclarationMemberReference(
                                    (MemberReferenceTree) expression, pair.second, enclosing)
                            .getThrownTypes();
        }

        for (AnnotatedTypeMirror xi : thrownTypes) {
            boolean isSubtypeOfProper = false;
            for (ProperAnnotatedType properType : properTypes) {
                if (typeFactory.getTypeHierarchy().isSubtype(xi, properType.getAnnotatedType())) {
                    isSubtypeOfProper = true;
                }
            }
            if (!isSubtypeOfProper) {
                for (Variable ei : es) {
                    constraintSet.add(
                            new Typing(
                                    new ProperAnnotatedType(xi, context),
                                    ei,
                                    Constraint.Kind.SUBTYPE));
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
        AnnotatedWildcardType wildcardAtm =
                (AnnotatedWildcardType)
                        AnnotatedTypeMirror.createType(wildcard, typeFactory, false);
        wildcardAtm.setSuperBound(((ProperAnnotatedType) lowerBound).getAnnotatedType());
        wildcardAtm.setExtendsBound(((AbstractAnnotatedType) upperBound).getAnnotatedType());
        return new ProperAnnotatedType(wildcardAtm, context);
    }

    @Override
    public List<ProperType> getSubsTypeArgs(
            List<TypeVariable> typeVar, List<ProperType> typeArg, List<Variable> asList) {
        throw new RuntimeException("Not implemented");
        //        List<TypeMirror> javaTypeArgs = new ArrayList<>();
        //        // Recursive types:
        //        for (int i = 0; i < typeArg.size(); i++) {
        //            Variable ai = asList.get(i);
        //            TypeMirror inst = typeArg.get(i).getJavaType();
        //            TypeVariable typeVariableI = ai.getJavaType();
        //            if (ContainsInferenceVariable.hasAnyTypeVariable(
        //                    Collections.singleton(typeVariableI), inst)) {
        //                // If the instantiation of ai includes a reference to ai,
        //                // then substitute ai with an unbound wildcard.  This isn't quite right but I'm not
        //                // sure how to make recursive types Java types.
        //                // TODO: This causes problems when incorporating the bounds.
        //                TypeMirror unbound = context.env.getTypeUtils().getWildcardType(null, null);
        //                inst =
        //                        TypesUtils.substitute(
        //                                inst,
        //                                Collections.singletonList(typeVariableI),
        //                                Collections.singletonList(unbound),
        //                                context.env);
        //                javaTypeArgs.add(inst);
        //            } else {
        //                javaTypeArgs.add(inst);
        //            }
        //        }
        //
        //        // Instantiations that refer to another variable
        //        List<ProperType> subsTypeArg = new ArrayList<>();
        //        for (TypeMirror type : javaTypeArgs) {
        //            TypeMirror subs = TypesUtils.substitute(type, typeVar, javaTypeArgs, context.env);
        //            subsTypeArg.add(new ProperTypeMirror(subs, context));
        //        }
        //        return subsTypeArg;
    }

    public static TypeMirror lub(
            ProcessingEnvironment processingEnv, TypeMirror tm1, TypeMirror tm2) {
        Type t1 = TypeAnnotationUtils.unannotatedType(tm1);
        Type t2 = TypeAnnotationUtils.unannotatedType(tm2);
        JavacProcessingEnvironment javacEnv = (JavacProcessingEnvironment) processingEnv;
        Types types = Types.instance(javacEnv.getContext());

        return types.lub(t1, t2);
    }

    public static TypeMirror glb(
            ProcessingEnvironment processingEnv, TypeMirror tm1, TypeMirror tm2) {
        Type t1 = TypeAnnotationUtils.unannotatedType(tm1);
        Type t2 = TypeAnnotationUtils.unannotatedType(tm2);
        JavacProcessingEnvironment javacEnv = (JavacProcessingEnvironment) processingEnv;
        Types types = Types.instance(javacEnv.getContext());

        return types.glb(t1, t2);
    }
}
