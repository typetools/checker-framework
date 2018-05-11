package org.checkerframework.framework.util.typeinference8.types;

import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MemberReferenceTree.ReferenceMode;
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
import com.sun.tools.javac.tree.JCTree.JCMemberReference;
import com.sun.tools.javac.tree.JCTree.JCMemberReference.ReferenceKind;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.framework.util.typeinference8.constraint.Constraint;
import org.checkerframework.framework.util.typeinference8.constraint.ConstraintSet;
import org.checkerframework.framework.util.typeinference8.constraint.Typing;
import org.checkerframework.framework.util.typeinference8.util.CheckedExceptionsUtil;
import org.checkerframework.framework.util.typeinference8.util.Java8InferenceContext;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.ErrorReporter;
import org.checkerframework.javacutil.Pair;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypeAnnotationUtils;
import org.checkerframework.javacutil.TypesUtils;

public class InferenceFactory {
    private Java8InferenceContext context;
    private final AnnotatedTypeFactory typeFactory;

    public InferenceFactory(Java8InferenceContext context) {
        this.context = context;
        this.typeFactory = context.typeFactory;
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
    public Theta createTheta(
            ExpressionTree invocation, InvocationType methodType, Java8InferenceContext context) {
        if (context.maps.containsKey(invocation)) {
            return context.maps.get(invocation);
        }
        Theta map = new Theta();
        Iterator<? extends AnnotatedTypeVariable> iter1 =
                methodType.getAnnotatedTypeVariables().iterator();
        for (TypeVariable pl : methodType.getTypeVariables()) {
            Variable al = new Variable(iter1.next(), pl, invocation, context);
            map.put(pl, al);
        }
        if (TreeUtils.isDiamondTree(invocation)) {
            Element classEle =
                    ElementUtils.enclosingClass(
                            TreeUtils.elementFromUse((NewClassTree) invocation));
            DeclaredType classTypeMirror = (DeclaredType) classEle.asType();

            AnnotatedDeclaredType classType =
                    (AnnotatedDeclaredType) typeFactory.getAnnotatedType(classEle);

            Iterator<AnnotatedTypeMirror> iter = classType.getTypeArguments().iterator();

            for (TypeMirror typeMirror : classTypeMirror.getTypeArguments()) {
                if (typeMirror.getKind() != TypeKind.TYPEVAR) {
                    ErrorReporter.errorAbort("Expected type variable, found: %s", typeMirror);
                    return map;
                }
                TypeVariable pl = (TypeVariable) typeMirror;
                AnnotatedTypeVariable atv = (AnnotatedTypeVariable) iter.next();
                Variable al = new Variable(atv, pl, invocation, context);
                map.put(pl, al);
            }
        }

        for (Variable v : map.values()) {
            v.initialBounds(map);
        }
        context.maps.put(invocation, map);
        return map;
    }

    public Theta createTheta(
            MemberReferenceTree memRef, InvocationType methodType, Java8InferenceContext context) {
        if (context.maps.containsKey(memRef)) {
            return context.maps.get(memRef);
        }

        Theta map = new Theta();
        if (TreeUtils.isDiamondMemberReference(memRef)) {
            TypeMirror type = TreeUtils.typeOf(memRef.getQualifierExpression());
            TypeElement classEle = (TypeElement) ((Type) type).asElement();
            DeclaredType classTypeMirror = (DeclaredType) classEle.asType();

            AnnotatedDeclaredType classType =
                    (AnnotatedDeclaredType)
                            typeFactory.getAnnotatedType(classTypeMirror.asElement());

            Iterator<AnnotatedTypeMirror> iter = classType.getTypeArguments().iterator();
            for (TypeMirror typeMirror : classTypeMirror.getTypeArguments()) {
                if (typeMirror.getKind() != TypeKind.TYPEVAR) {
                    ErrorReporter.errorAbort("Expected type variable, found: %s", typeMirror);
                    return map;
                }
                TypeVariable pl = (TypeVariable) typeMirror;
                AnnotatedTypeVariable atv = (AnnotatedTypeVariable) iter.next();
                Variable al = new Variable(atv, pl, memRef, context);
                map.put(pl, al);
            }
        }
        if (memRef.getTypeArguments() == null && methodType.hasTypeVariables()) {
            Iterator<? extends AnnotatedTypeVariable> iter1 =
                    methodType.getAnnotatedTypeVariables().iterator();
            for (TypeVariable pl : methodType.getTypeVariables()) {
                Variable al = new Variable(iter1.next(), pl, memRef, context);
                map.put(pl, al);
            }
        }
        for (Variable v : map.values()) {
            v.initialBounds(map);
        }
        context.maps.put(memRef, map);
        return map;
    }

    public Theta createTheta(LambdaExpressionTree lambda, AbstractType t) {
        if (context.maps.containsKey(lambda)) {
            return context.maps.get(lambda);
        }
        TypeElement typeEle = (TypeElement) ((DeclaredType) t.getJavaType()).asElement();
        AnnotatedDeclaredType classType = typeFactory.getAnnotatedType(typeEle);

        Iterator<AnnotatedTypeMirror> iter = classType.getTypeArguments().iterator();
        Theta map = new Theta();
        for (TypeParameterElement param : typeEle.getTypeParameters()) {
            TypeVariable typeVar = (TypeVariable) param.asType();
            AnnotatedTypeVariable atv = (AnnotatedTypeVariable) iter.next();
            Variable ai = new Variable(atv, typeVar, lambda, context);
            map.put(typeVar, ai);
        }
        for (Variable v : map.values()) {
            v.initialBounds(map);
        }
        context.maps.put(lambda, map);
        return map;
    }

    public Theta createThetaForCapture(ExpressionTree tree, AbstractType capturedType) {
        // Don't save this theta, because there is also a noncapture theta for this tree.
        DeclaredType underlying = (DeclaredType) capturedType.getJavaType();
        TypeElement ele = TypesUtils.getTypeElement(underlying);
        AnnotatedDeclaredType classType = typeFactory.getAnnotatedType(ele);
        Iterator<AnnotatedTypeMirror> iter = classType.getTypeArguments().iterator();
        Theta map = new Theta();
        for (TypeParameterElement pEle : ele.getTypeParameters()) {
            TypeVariable pl = (TypeVariable) pEle.asType();
            AnnotatedTypeVariable atv = (AnnotatedTypeVariable) iter.next();
            CaptureVariable al = new CaptureVariable(atv, pl, tree, context);
            map.put(pl, al);
        }
        for (Variable v : map.values()) {
            v.initialBounds(map);
        }
        return map;
    }

    public InvocationType getTypeOfMethodAdaptedToUse(ExpressionTree invocation) {
        AnnotatedExecutableType executableType;
        if (invocation.getKind() == Kind.METHOD_INVOCATION) {
            executableType = typeFactory.methodFromUse((MethodInvocationTree) invocation).first;
        } else {
            executableType = typeFactory.constructorFromUse((NewClassTree) invocation).first;
        }
        return new InvocationType(
                executableType,
                getTypeOfMethodAdaptedToUse(invocation, context),
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
    public static Pair<AnnotatedTypeMirror, TypeMirror> getTargetType(
            AnnotatedTypeFactory factory, TreePath path, Java8InferenceContext context) {
        Tree assignmentContext = TreeUtils.getAssignmentContext(path);
        if (assignmentContext == null) {
            return null;
        }

        switch (assignmentContext.getKind()) {
            case ASSIGNMENT:
                ExpressionTree variable = ((AssignmentTree) assignmentContext).getVariable();
                AnnotatedTypeMirror atm = factory.getAnnotatedType(variable);
                return Pair.of(atm, TreeUtils.typeOf(variable));
            case VARIABLE:
                VariableTree variableTree = (VariableTree) assignmentContext;
                AnnotatedTypeMirror variableAtm = assignedToVariable(factory, assignmentContext);
                return Pair.of(variableAtm, TreeUtils.typeOf(variableTree.getType()));
            case METHOD_INVOCATION:
                MethodInvocationTree methodInvocation = (MethodInvocationTree) assignmentContext;
                ExecutableElement methodElt = TreeUtils.elementFromUse(methodInvocation);
                AnnotatedTypeMirror receiver = factory.getReceiverType(methodInvocation);
                AnnotatedTypeMirror ex =
                        assignedToExecutable(
                                path,
                                methodInvocation,
                                methodInvocation.getArguments(),
                                receiver,
                                factory,
                                methodElt);
                return Pair.of(
                        ex,
                        assignedToExecutable(
                                path, methodInvocation, methodInvocation.getArguments(), context));
            case NEW_CLASS:
                NewClassTree newClassTree = (NewClassTree) assignmentContext;
                ExecutableElement constructorElt = TreeUtils.constructor(newClassTree);
                AnnotatedTypeMirror receiverConst = factory.fromNewClass(newClassTree);
                AnnotatedTypeMirror constATM =
                        assignedToExecutable(
                                path,
                                newClassTree,
                                newClassTree.getArguments(),
                                receiverConst,
                                factory,
                                constructorElt);
                return Pair.of(
                        constATM,
                        assignedToExecutable(
                                path, newClassTree, newClassTree.getArguments(), context));
            case NEW_ARRAY:
                NewArrayTree newArrayTree = (NewArrayTree) assignmentContext;
                ArrayType arrayType = (ArrayType) TreeUtils.typeOf(newArrayTree);
                AnnotatedTypeMirror type =
                        factory.getAnnotatedType((NewArrayTree) assignmentContext);
                AnnotatedTypeMirror component =
                        ((AnnotatedTypeMirror.AnnotatedArrayType) type).getComponentType();
                return Pair.of(component, arrayType.getComponentType());
            case RETURN:
                HashSet<Kind> kinds =
                        new HashSet<>(Arrays.asList(Tree.Kind.LAMBDA_EXPRESSION, Tree.Kind.METHOD));
                Tree enclosing = TreeUtils.enclosingOfKind(path, kinds);
                if (enclosing.getKind() == Tree.Kind.METHOD) {
                    MethodTree methodTree = (MethodTree) enclosing;
                    AnnotatedTypeMirror res = factory.getAnnotatedType(methodTree).getReturnType();
                    return Pair.of(res, TreeUtils.typeOf(methodTree.getReturnType()));
                } else {
                    // TODO: I don't think this should happen. during inference
                    LambdaExpressionTree lambdaTree = (LambdaExpressionTree) enclosing;
                    Pair<AnnotatedDeclaredType, AnnotatedExecutableType> fninf =
                            factory.getFnInterfaceFromTree((LambdaExpressionTree) enclosing);
                    AnnotatedTypeMirror res = fninf.second.getReturnType();
                    return Pair.of(res, TreeUtils.typeOf(lambdaTree));
                }
            default:
                if (assignmentContext
                        .getKind()
                        .asInterface()
                        .equals(CompoundAssignmentTree.class)) {
                    // 11 Tree kinds are compound assignments, so don't use it in the switch
                    ExpressionTree var = ((CompoundAssignmentTree) assignmentContext).getVariable();
                    AnnotatedTypeMirror res = factory.getAnnotatedType(var);

                    return Pair.of(res, TreeUtils.typeOf(var));
                } else {
                    ErrorReporter.errorAbort(
                            "Unexpected assignment context.\nKind: %s\nTree: %s",
                            assignmentContext.getKind(), assignmentContext);
                    return null;
                }
        }
    }

    /**
     * If the variable's type is a type variable, return getAnnotatedTypeLhsNoTypeVarDefault(tree).
     * Rational:
     *
     * <p>For example:
     *
     * <pre>{@code
     * <S> S bar () {...}
     *
     * <T> T foo(T p) {
     *     T local = bar();
     *     return local;
     *   }
     * }</pre>
     *
     * During type argument inference of {@code bar}, the assignment context is {@code local}. If
     * the local variable default is used, then the type of assignment context type is
     * {@code @Nullable T} and the type argument inferred for {@code bar()} is {@code @Nullable T}.
     * And an incompatible types in return error is issued.
     *
     * <p>If instead, the local variable default is not applied, then the assignment context type is
     * {@code T} (with lower bound {@code @NonNull Void} and upper bound {@code @Nullable Object})
     * and the type argument inferred for {@code bar()} is {@code T}. During dataflow, the type of
     * {@code local} is refined to {@code T} and the return is legal.
     *
     * <p>If the assignment context type was a declared type, for example:
     *
     * <pre>{@code
     * <S> S bar () {...}
     * Object foo() {
     *     Object local = bar();
     *     return local;
     * }
     * }</pre>
     *
     * The local variable default must be used or else the assignment context type is missing an
     * annotation. So, an incompatible types in return error is issued in the above code. We could
     * improve type argument inference in this case and by using the lower bound of {@code S}
     * instead of the local variable default.
     *
     * @param atypeFactory AnnotatedTypeFactory
     * @param assignmentContext VariableTree
     * @return AnnotatedTypeMirror of Assignment context
     */
    public static AnnotatedTypeMirror assignedToVariable(
            AnnotatedTypeFactory atypeFactory, Tree assignmentContext) {
        if (atypeFactory instanceof GenericAnnotatedTypeFactory<?, ?, ?, ?>) {
            final GenericAnnotatedTypeFactory<?, ?, ?, ?> gatf =
                    ((GenericAnnotatedTypeFactory<?, ?, ?, ?>) atypeFactory);
            return gatf.getAnnotatedTypeLhsNoTypeVarDefault(assignmentContext);
        } else {
            return atypeFactory.getAnnotatedType(assignmentContext);
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

    private static AnnotatedTypeMirror assignedToExecutable(
            TreePath path,
            ExpressionTree methodInvocation,
            List<? extends ExpressionTree> arguments,
            AnnotatedTypeMirror receiver,
            AnnotatedTypeFactory atypeFactory,
            ExecutableElement methodElt) {
        int treeIndex = -1;
        for (int i = 0; i < arguments.size(); ++i) {
            ExpressionTree argumentTree = arguments.get(i);
            if (isArgument(path, argumentTree)) {
                treeIndex = i;
                break;
            }
        }

        AnnotatedExecutableType methodType =
                AnnotatedTypes.asMemberOf(
                        atypeFactory.getContext().getTypeUtils(),
                        atypeFactory,
                        receiver,
                        methodElt);
        if (treeIndex >= methodType.getParameterTypes().size() - 1
                && TreeUtils.isVarArgMethodCall(methodInvocation)) {
            treeIndex = methodType.getParameterTypes().size() - 1;
            AnnotatedTypeMirror typeMirror = methodType.getParameterTypes().get(treeIndex);
            return ((AnnotatedArrayType) typeMirror).getComponentType();
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
                return (ExecutableType) TreeUtils.elementFromUse(expressionTree).asType();
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

    public ProperType getTargetType() {
        ProperType targetType = null;
        Pair<AnnotatedTypeMirror, TypeMirror> assignmentTypes =
                getTargetType(context.typeFactory, context.pathToExpression, context);

        if (assignmentTypes != null) {
            targetType = new ProperType(assignmentTypes.first, assignmentTypes.second, context);
        }
        return targetType;
    }

    public InvocationType compileTimeDeclarationType(
            MemberReferenceTree memRef, AbstractType targetType) {
        // The type of the expression or type use, <expression>::method or <type use>::method.
        final ExpressionTree qualifierExpression = memRef.getQualifierExpression();
        final ReferenceKind memRefKind = ((JCMemberReference) memRef).kind;
        AnnotatedTypeMirror enclosingType;

        if (memRef.getMode() == ReferenceMode.NEW) {
            enclosingType = typeFactory.getAnnotatedTypeFromTypeTree(qualifierExpression);
            if (enclosingType.getKind() == TypeKind.DECLARED
                    && ((AnnotatedDeclaredType) enclosingType).wasRaw()) {
                // The member reference is HashMap::new so the type arguments for HashMap must be inferred.
                // So use the type declared type.
                TypeElement typeEle = TypesUtils.getTypeElement(enclosingType.getUnderlyingType());
                enclosingType = typeFactory.getAnnotatedType(typeEle);
            }
        } else if (memRefKind == ReferenceKind.UNBOUND) {
            enclosingType = typeFactory.getAnnotatedTypeFromTypeTree(qualifierExpression);
            if (enclosingType.getKind() == TypeKind.DECLARED
                    && ((AnnotatedDeclaredType) enclosingType).wasRaw()) {
                List<AbstractType> params = targetType.getFunctionTypeParameterTypes();
                if (params.size() > 0) {
                    enclosingType = params.get(0).getAnnotatedType();
                }
            }
        } else if (memRefKind == ReferenceKind.STATIC) {
            // The "qualifier expression" is a type tree.
            enclosingType = typeFactory.getAnnotatedTypeFromTypeTree(qualifierExpression);
        } else {
            // The "qualifier expression" is an expression.
            enclosingType = typeFactory.getAnnotatedType(qualifierExpression);
        }

        // The ::method element, see JLS 15.13.1 Compile-Time Declaration of a Method Reference
        ExecutableElement compileTimeDeclaration =
                (ExecutableElement) TreeUtils.elementFromTree(memRef);

        if (enclosingType.getKind() == TypeKind.DECLARED) {
            AbstractType.makeGround((AnnotatedDeclaredType) enclosingType, typeFactory);
        }
        // The type of the compileTimeDeclaration if it were invoked with a receiver expression
        // of type {@code type}
        AnnotatedExecutableType compileTimeType =
                typeFactory.methodFromUse(memRef, compileTimeDeclaration, enclosingType).first;

        return new InvocationType(
                compileTimeType,
                TreeUtils.compileTimeDeclarationType(memRef, targetType.getJavaType(), context.env),
                memRef,
                context);
    }

    public InvocationType findFunctionType(MemberReferenceTree memRef, AbstractType targetType) {
        InvocationType other = compileTimeDeclarationType(memRef, targetType);

        // The type of the single method that is declared by the functional interface.
        AnnotatedExecutableType functionType = other.getAnnotatedType();
        return new InvocationType(
                functionType,
                TypesUtils.findFunctionType(TreeUtils.typeOf(memRef), context.env),
                memRef,
                context);
    }

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

        return Pair.of(a.asSuper(asSuperOfA), b.asSuper(asSuperOfB));
    }

    public ProperType getTypeOfExpression(ExpressionTree tree) {
        return new ProperType(tree, context);
    }

    public ProperType getTypeOfVariable(VariableTree tree) {
        return new ProperType(tree, context);
    }

    public AbstractType getTypeOfElement(Element element, Theta map) {
        AnnotatedTypeMirror atm = typeFactory.getAnnotatedType(element);
        return InferenceType.create(atm, element.asType(), map, context);
    }

    public AbstractType getTypeOfBound(TypeParameterElement pEle, Theta map) {
        AnnotatedTypeVariable atm = (AnnotatedTypeVariable) typeFactory.getAnnotatedType(pEle);
        return InferenceType.create(
                atm.getUpperBound(), ((TypeVariable) pEle.asType()).getUpperBound(), map, context);
    }

    public ProperType getObject() {
        TypeMirror objectTypeMirror =
                TypesUtils.typeFromClass(
                        Object.class, context.modelTypes, context.env.getElementUtils());
        AnnotatedTypeMirror object =
                AnnotatedTypeMirror.createType(objectTypeMirror, typeFactory, false);
        object.addMissingAnnotations(typeFactory.getQualifierHierarchy().getTopAnnotations());
        return new ProperType(object, objectTypeMirror, context);
    }

    public ProperType lub(LinkedHashSet<ProperType> lowerBounds) {
        if (lowerBounds.isEmpty()) {
            return null;
        }
        TypeMirror tiTypeMirror = null;
        AnnotatedTypeMirror ti = null;
        for (ProperType liProperType : lowerBounds) {
            AnnotatedTypeMirror li = liProperType.getAnnotatedType();
            TypeMirror liTypeMirror = liProperType.getJavaType();
            if (ti == null) {
                ti = li;
                tiTypeMirror = liTypeMirror;
            } else {
                tiTypeMirror = lub(context.env, tiTypeMirror, liTypeMirror);
                ti = AnnotatedTypes.leastUpperBound(typeFactory, ti, li, tiTypeMirror);
            }
        }
        return new ProperType(ti, tiTypeMirror, context);
    }

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

    public AbstractType glb(AbstractType a, AbstractType b) {
        Type aJavaType = (Type) a.getJavaType();
        Type bJavaType = (Type) b.getJavaType();
        TypeMirror glb = TypesUtils.greatestLowerBound(aJavaType, bJavaType, context.env);

        AnnotatedTypeMirror aAtm = a.getAnnotatedType();
        AnnotatedTypeMirror bAtm = b.getAnnotatedType();
        AnnotatedTypeMirror glbATM = AnnotatedTypes.greatestLowerBound(typeFactory, aAtm, bAtm);
        if (context.types.isSameType(aJavaType, (Type) glb)) {
            return a;
        }

        if (context.types.isSameType(bJavaType, (Type) glb)) {
            return b;
        }

        if (a.isInferenceType()) {
            return a.create(glbATM, glb);
        } else if (b.isInferenceType()) {
            return b.create(glbATM, glb);
        }

        assert a.isProper() && b.isProper();
        return new ProperType(glbATM, glb, context);
    }

    public ProperType getRuntimeException() {
        AnnotatedTypeMirror runtimeEx =
                AnnotatedTypeMirror.createType(context.runtimeEx, typeFactory, false);
        runtimeEx.addMissingAnnotations(typeFactory.getQualifierHierarchy().getTopAnnotations());
        return new ProperType(runtimeEx, context.runtimeEx, context);
    }

    public ConstraintSet getCheckedExceptionConstraints(
            ExpressionTree expression, AbstractType targetType, Theta map) {
        ConstraintSet constraintSet = new ConstraintSet();
        ExecutableElement ele = (ExecutableElement) TreeUtils.findFunction(expression, context.env);
        List<Variable> es = new ArrayList<>();
        List<ProperType> properTypes = new ArrayList<>();

        AnnotatedExecutableType aet;
        if (expression.getKind() == Kind.LAMBDA_EXPRESSION) {
            aet = typeFactory.getFnInterfaceFromTree((LambdaExpressionTree) expression).second;
        } else {
            aet = findFunctionType((MemberReferenceTree) expression, targetType).getAnnotatedType();
        }
        Iterator<AnnotatedTypeMirror> iter = aet.getThrownTypes().iterator();
        for (TypeMirror thrownType : ele.getThrownTypes()) {
            AbstractType ei = InferenceType.create(iter.next(), thrownType, map, context);
            if (ei.isProper()) {
                properTypes.add((ProperType) ei);
            } else {
                es.add((Variable) ei);
            }
        }
        if (es.isEmpty()) {
            return ConstraintSet.TRUE;
        }
        List<? extends AnnotatedTypeMirror> thrownTypes;
        List<? extends TypeMirror> thrownTypeMirrors;
        if (expression.getKind() == Tree.Kind.LAMBDA_EXPRESSION) {
            thrownTypeMirrors =
                    CheckedExceptionsUtil.thrownCheckedExceptions(
                            (LambdaExpressionTree) expression, context);
            thrownTypes =
                    org.checkerframework.framework.util.typeinference8.CheckedExceptionsUtil
                            .thrownCheckedExceptions((LambdaExpressionTree) expression, context);
        } else {
            thrownTypeMirrors =
                    TypesUtils.findFunctionType(TreeUtils.typeOf(expression), context.env)
                            .getThrownTypes();
            thrownTypes =
                    compileTimeDeclarationType((MemberReferenceTree) expression, targetType)
                            .getAnnotatedType()
                            .getThrownTypes();
        }

        Iterator<? extends AnnotatedTypeMirror> iter2 = thrownTypes.iterator();
        for (TypeMirror xi : thrownTypeMirrors) {
            boolean isSubtypeOfProper = false;
            for (ProperType properType : properTypes) {
                if (context.env.getTypeUtils().isSubtype(xi, properType.getJavaType())) {
                    isSubtypeOfProper = true;
                }
            }
            if (!isSubtypeOfProper) {
                for (Variable ei : es) {
                    constraintSet.add(
                            new Typing(
                                    new ProperType(iter2.next(), xi, context),
                                    ei,
                                    Constraint.Kind.SUBTYPE));
                    ei.getBounds().setHasThrowsBound(true);
                }
            }
        }

        return constraintSet;
    }

    public ProperType createWildcard(ProperType lowerBound, AbstractType upperBound) {
        TypeMirror wildcard =
                TypesUtils.createWildcard(
                        lowerBound == null ? null : lowerBound.getJavaType(),
                        upperBound == null ? null : upperBound.getJavaType(),
                        context.env.getTypeUtils());
        AnnotatedWildcardType wildcardAtm =
                (AnnotatedWildcardType)
                        AnnotatedTypeMirror.createType(wildcard, typeFactory, false);
        if (lowerBound != null) {
            wildcardAtm.setSuperBound(lowerBound.getAnnotatedType());
        }
        if (upperBound != null) {
            wildcardAtm.setExtendsBound(upperBound.getAnnotatedType());
        }
        return new ProperType(wildcardAtm, wildcard, context);
    }

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

        for (int i = 0; i < typeVar.size(); i++) {
            TypeMirror javaTypeArg = javaTypeArgs.get(i);
            TypeMirror x = TypesUtils.substitute(javaTypeArg, typeVar, javaTypeArgs, context.env);
            javaTypeArgs.remove(i);
            javaTypeArgs.add(i, x);
        }

        Map<TypeVariable, AnnotatedTypeMirror> map = new HashMap<>();

        List<AnnotatedTypeMirror> typeArgsATM = new ArrayList<>();
        // Recursive types:
        for (int i = 0; i < typeArg.size(); i++) {
            Variable ai = asList.get(i);
            ProperType inst = typeArg.get(i);
            typeArgsATM.add(inst.getAnnotatedType());
            TypeVariable typeVariableI = ai.getJavaType();
            map.put(typeVariableI, inst.getAnnotatedType());
        }

        Iterator<TypeMirror> iter = javaTypeArgs.iterator();
        // Instantiations that refer to another variable
        List<ProperType> subsTypeArg = new ArrayList<>();
        for (AnnotatedTypeMirror type : typeArgsATM) {
            AnnotatedTypeMirror subs = typeFactory.getTypeVarSubstitutor().substitute(map, type);
            subsTypeArg.add(new ProperType(subs, iter.next(), context));
        }
        return subsTypeArg;
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
