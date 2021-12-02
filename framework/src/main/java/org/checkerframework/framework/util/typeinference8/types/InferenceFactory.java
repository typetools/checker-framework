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
import org.checkerframework.checker.nullness.qual.Nullable;
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
import org.checkerframework.framework.util.typeinference8.util.Theta;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.Pair;
import org.checkerframework.javacutil.TreePathUtil;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypeAnnotationUtils;
import org.checkerframework.javacutil.TypesUtils;

/** Factory that creates AbstractTypes. */
public class InferenceFactory {

  /** AnnotatedTypeFactory used to get annotated types. */
  private final AnnotatedTypeFactory typeFactory;
  /** Stores information about the current inference problem being solved. */
  private Java8InferenceContext context;

  public InferenceFactory(Java8InferenceContext context) {
    this.context = context;
    this.typeFactory = context.typeFactory;
  }

  /**
   * Gets the target type for the expression for which type arguments are being inferred.
   *
   * @return target type for the expression for which type arguments are being inferred
   */
  public @Nullable ProperType getTargetType() {
    AnnotatedTypeFactory factory = context.typeFactory;
    TreePath path = context.pathToExpression;
    Tree assignmentContext = TreePathUtil.getAssignmentContext(path);
    if (assignmentContext == null) {
      return null;
    }

    switch (assignmentContext.getKind()) {
      case ASSIGNMENT:
        ExpressionTree variable = ((AssignmentTree) assignmentContext).getVariable();
        AnnotatedTypeMirror atm = factory.getAnnotatedType(variable);
        return new ProperType(atm, TreeUtils.typeOf(variable), context);
      case VARIABLE:
        VariableTree variableTree = (VariableTree) assignmentContext;
        AnnotatedTypeMirror variableAtm = assignedToVariable(factory, assignmentContext);
        return new ProperType(variableAtm, TreeUtils.typeOf(variableTree.getType()), context);
      case METHOD_INVOCATION:
        MethodInvocationTree methodInvocation = (MethodInvocationTree) assignmentContext;

        AnnotatedExecutableType methodType =
            factory.methodFromUseNoTypeArgInfere(methodInvocation).executableType;

        AnnotatedTypeMirror ex =
            assignedToExecutable(
                path, methodInvocation, methodInvocation.getArguments(), methodType);
        return new ProperType(
            ex,
            assignedToExecutable(path, methodInvocation, methodInvocation.getArguments(), context),
            context);
      case NEW_CLASS:
        NewClassTree newClassTree = (NewClassTree) assignmentContext;
        AnnotatedExecutableType constructorType =
            factory.constructorFromUse(newClassTree).executableType;
        AnnotatedTypeMirror constATM =
            assignedToExecutable(path, newClassTree, newClassTree.getArguments(), constructorType);
        return new ProperType(
            constATM,
            assignedToExecutable(path, newClassTree, newClassTree.getArguments(), context),
            context);
      case NEW_ARRAY:
        NewArrayTree newArrayTree = (NewArrayTree) assignmentContext;
        ArrayType arrayType = (ArrayType) TreeUtils.typeOf(newArrayTree);
        AnnotatedArrayType type = factory.getAnnotatedType((NewArrayTree) assignmentContext);
        AnnotatedTypeMirror component = type.getComponentType();
        return new ProperType(component, arrayType.getComponentType(), context);
      case RETURN:
        HashSet<Kind> kinds =
            new HashSet<>(Arrays.asList(Tree.Kind.LAMBDA_EXPRESSION, Tree.Kind.METHOD));
        Tree enclosing = TreePathUtil.enclosingOfKind(path, kinds);
        if (enclosing.getKind() == Tree.Kind.METHOD) {
          MethodTree methodTree = (MethodTree) enclosing;
          AnnotatedTypeMirror res = factory.getAnnotatedType(methodTree).getReturnType();
          return new ProperType(res, TreeUtils.typeOf(methodTree.getReturnType()), context);
        } else {
          // TODO: I don't think this should happen. during inference
          LambdaExpressionTree lambdaTree = (LambdaExpressionTree) enclosing;
          AnnotatedExecutableType fninf =
              factory.getFunctionTypeFromTree((LambdaExpressionTree) enclosing);
          AnnotatedTypeMirror res = fninf.getReturnType();
          return new ProperType(res, TreeUtils.typeOf(lambdaTree), context);
        }
      default:
        if (assignmentContext.getKind().asInterface().equals(CompoundAssignmentTree.class)) {
          // 11 Tree kinds are compound assignments, so don't use it in the switch
          ExpressionTree var = ((CompoundAssignmentTree) assignmentContext).getVariable();
          AnnotatedTypeMirror res = factory.getAnnotatedType(var);

          return new ProperType(res, TreeUtils.typeOf(var), context);
        } else {
          throw new BugInCF(
              "Unexpected assignment context.\nKind: %s\nTree: %s",
              assignmentContext.getKind(), assignmentContext);
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
   * During type argument inference of {@code bar}, the assignment context is {@code local}. If the
   * local variable default is used, then the type of assignment context type is {@code @Nullable T}
   * and the type argument inferred for {@code bar()} is {@code @Nullable T}. And an incompatible
   * types in return error is issued.
   *
   * <p>If instead, the local variable default is not applied, then the assignment context type is
   * {@code T} (with lower bound {@code @NonNull Void} and upper bound {@code @Nullable Object}) and
   * the type argument inferred for {@code bar()} is {@code T}. During dataflow, the type of {@code
   * local} is refined to {@code T} and the return is legal.
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
   * improve type argument inference in this case and by using the lower bound of {@code S} instead
   * of the local variable default.
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
      AnnotatedExecutableType methodType) {
    int treeIndex = -1;
    for (int i = 0; i < arguments.size(); ++i) {
      ExpressionTree argumentTree = arguments.get(i);
      if (isArgument(path, argumentTree)) {
        treeIndex = i;
        break;
      }
    }

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
    argumentTree = TreeUtils.withoutParens(argumentTree);
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
   * Return ExecutableType of the method invocation or new class tree adapted to the call site.
   *
   * @return ExecutableType of the method invocation or new class tree adapted to the call site
   */
  public static ExecutableType getTypeOfMethodAdaptedToUse(
      ExpressionTree expressionTree, Java8InferenceContext context) {
    if (expressionTree.getKind() == Tree.Kind.NEW_CLASS) {
      if (!TreeUtils.isDiamondTree(expressionTree)) {
        NewClassTree newClassTree = (NewClassTree) expressionTree;
        ExecutableType type = (ExecutableType) TreeUtils.elementFromUse(expressionTree).asType();
        List<? extends Tree> typeArgs = TreeUtils.getTypeArgumentsToNewClassTree(newClassTree);
        if (!typeArgs.isEmpty()) {
          ExecutableElement e = TreeUtils.constructor(newClassTree);
          List<? extends TypeParameterElement> typeParams =
              ElementUtils.enclosingTypeElement(e).getTypeParameters();
          List<TypeVariable> typeVariables = new ArrayList<>();
          for (TypeParameterElement typeParam : typeParams) {
            typeVariables.add((TypeVariable) typeParam.asType());
          }

          List<TypeMirror> args = new ArrayList<>();
          for (Tree arg : typeArgs) {
            args.add(TreeUtils.typeOf(arg));
          }

          return (ExecutableType) TypesUtils.substitute(type, typeVariables, args, context.env);
        }

        return type;
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
    } else {
      receiverType = (DeclaredType) context.types.capture((Type) receiverType);
    }

    while (context.types.asSuper((Type) receiverType, (Symbol) ele.getEnclosingElement()) == null) {
      TypeMirror enclosing = receiverType.getEnclosingType();
      if (enclosing == null || enclosing.getKind() != TypeKind.DECLARED) {
        if (expressionTree.getKind() == Tree.Kind.NEW_CLASS) {
          // No receiver for the constructor.
          return (ExecutableType) ele.asType();
        } else {
          throw new BugInCF("Method not found");
        }
      }
      receiverType = (DeclaredType) enclosing;
    }
    javax.lang.model.util.Types types = context.env.getTypeUtils();
    return (ExecutableType) types.asMemberOf(receiverType, ele);
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

  /**
   * If a mapping, theta, for {@code invocation} doesn't exist create it by:
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
  public Theta createThetaForInvocation(
      ExpressionTree invocation, InvocationType methodType, Java8InferenceContext context) {
    if (context.maps.containsKey(invocation)) {
      return context.maps.get(invocation);
    }
    Theta map = new Theta();

    // Create inference variables for the type parameters to methodType
    Iterator<? extends AnnotatedTypeVariable> iter1 =
        methodType.getAnnotatedTypeVariables().iterator();
    for (TypeVariable pl : methodType.getTypeVariables()) {
      Variable al = new Variable(iter1.next(), pl, invocation, context, map);
      map.put(pl, al);
    }
    if (TreeUtils.isDiamondTree(invocation)) {
      // If the invocation is a diamondTree, such as new List<>(...), then create variables
      // for the class type parameters, too.
      Element classEle =
          ElementUtils.enclosingTypeElement(TreeUtils.elementFromUse((NewClassTree) invocation));
      DeclaredType classTypeMirror = (DeclaredType) classEle.asType();

      AnnotatedDeclaredType classType =
          (AnnotatedDeclaredType) typeFactory.getAnnotatedType(classEle);

      Iterator<AnnotatedTypeMirror> iter = classType.getTypeArguments().iterator();

      for (TypeMirror typeMirror : classTypeMirror.getTypeArguments()) {
        if (typeMirror.getKind() != TypeKind.TYPEVAR) {
          throw new BugInCF("Expected type variable, found: %s", typeMirror);
        }
        TypeVariable pl = (TypeVariable) typeMirror;
        AnnotatedTypeVariable atv = (AnnotatedTypeVariable) iter.next();
        Variable al = new Variable(atv, pl, invocation, context, map);
        map.put(pl, al);
      }
    }

    // Initialize variable bounds.
    for (Variable v : map.values()) {
      v.initialBounds(map);
    }
    context.maps.put(invocation, map);
    return map;
  }

  /**
   * If a mapping, theta, for {@code memRef} doesn't exist create it by:
   *
   * <p>Creates inference variables for the type parameters to {@code compileTimeDecl} for a
   * particular method reference. Initializes the bounds of the variables. Returns a mapping from
   * type variables to newly created variables.
   *
   * <p>Otherwise, returns the previously created mapping.
   *
   * @param memRef method reference tree
   * @param compileTimeDecl type of generic method
   * @param context Java8InferenceContext
   * @return a mapping of the type variables of {@code compileTimeDecl} to inference variables
   */
  public Theta createThetaForMethodReference(
      MemberReferenceTree memRef, InvocationType compileTimeDecl, Java8InferenceContext context) {
    if (context.maps.containsKey(memRef)) {
      return context.maps.get(memRef);
    }

    Theta map = new Theta();
    if (TreeUtils.isDiamondMemberReference(memRef)) {
      // If memRef is a constructor of a generic class whose type argument isn't specified
      // such as HashSet::new,
      // then add variables for the type arguments to the class.
      TypeMirror type = TreeUtils.typeOf(memRef.getQualifierExpression());
      TypeElement classEle = (TypeElement) ((Type) type).asElement();
      DeclaredType classTypeMirror = (DeclaredType) classEle.asType();

      AnnotatedDeclaredType classType =
          (AnnotatedDeclaredType) typeFactory.getAnnotatedType(classTypeMirror.asElement());

      Iterator<AnnotatedTypeMirror> iter = classType.getTypeArguments().iterator();
      for (TypeMirror typeMirror : classTypeMirror.getTypeArguments()) {
        if (typeMirror.getKind() != TypeKind.TYPEVAR) {
          throw new BugInCF("Expected type variable, found: %s", typeMirror);
        }
        TypeVariable pl = (TypeVariable) typeMirror;
        AnnotatedTypeVariable atv = (AnnotatedTypeVariable) iter.next();
        Variable al = new Variable(atv, pl, memRef, context, map);
        map.put(pl, al);
      }
    }

    // Create inference variables for the type parameters to compileTypeDecl
    if (memRef.getTypeArguments() == null && compileTimeDecl.hasTypeVariables()) {
      Iterator<? extends AnnotatedTypeVariable> iter1 =
          compileTimeDecl.getAnnotatedTypeVariables().iterator();
      for (TypeVariable pl : compileTimeDecl.getTypeVariables()) {
        Variable al = new Variable(iter1.next(), pl, memRef, context, map);
        map.put(pl, al);
      }
    }
    for (Variable v : map.values()) {
      v.initialBounds(map);
    }
    context.maps.put(memRef, map);
    return map;
  }

  /**
   * If a mapping, theta, for {@code lambda} doesn't exist create it by:
   *
   * <p>Creates inference variables for the type parameters to the functional inference of the
   * lambda. Initializes the bounds of the variables. Returns a mapping from type variables to newly
   * created variables.
   *
   * <p>Otherwise, returns the previously created mapping.
   *
   * @param lambda lambda expression tree
   * @param functionalInterface functional interface of the lambda
   * @return a mapping of the type variables of {@code compileTimeDecl} to inference variables
   */
  public Theta createThetaForLambda(LambdaExpressionTree lambda, AbstractType functionalInterface) {
    if (context.maps.containsKey(lambda)) {
      return context.maps.get(lambda);
    }
    TypeElement typeEle =
        (TypeElement) ((DeclaredType) functionalInterface.getJavaType()).asElement();
    AnnotatedDeclaredType classType = typeFactory.getAnnotatedType(typeEle);

    Iterator<AnnotatedTypeMirror> iter = classType.getTypeArguments().iterator();
    Theta map = new Theta();
    for (TypeParameterElement param : typeEle.getTypeParameters()) {
      TypeVariable typeVar = (TypeVariable) param.asType();
      AnnotatedTypeVariable atv = (AnnotatedTypeVariable) iter.next();
      Variable ai = new Variable(atv, typeVar, lambda, context, map);
      map.put(typeVar, ai);
    }
    for (Variable v : map.values()) {
      v.initialBounds(map);
    }
    context.maps.put(lambda, map);
    return map;
  }

  /**
   * Creates capture variables for variables introduced by a capture bounds. The new variables
   * correspond to the type parameters of {@code capturedType}.
   *
   * @param tree invocation tree that created the capture bound
   * @param capturedType type that should be captured
   * @return a mapping of the type variables of {@code capturedType} to capture inference variables
   */
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
      CaptureVariable al = new CaptureVariable(atv, pl, tree, context, map);
      map.put(pl, al);
    }
    for (Variable v : map.values()) {
      v.initialBounds(map);
    }
    return map;
  }

  /**
   * Returns the type of the method or constructor invocation adapted to its arguments. This type
   * may include inference variables.
   *
   * @param invocation method or constructor invocation
   * @return the type of the method or constructor invocation adapted to its arguments
   */
  public InvocationType getTypeOfMethodAdaptedToUse(ExpressionTree invocation) {
    AnnotatedExecutableType executableType;
    if (invocation.getKind() == Kind.METHOD_INVOCATION) {
      executableType =
          typeFactory.methodFromUseNoTypeArgInfere((MethodInvocationTree) invocation)
              .executableType;
    } else {
      executableType = typeFactory.constructorFromUse((NewClassTree) invocation).executableType;
    }
    return new InvocationType(
        executableType, getTypeOfMethodAdaptedToUse(invocation, context), invocation, context);
  }

  /**
   * Returns the compile-time declaration of the method reference that is the method to which the
   * expression refers. See <a
   * href="https://docs.oracle.com/javase/specs/jls/se11/html/jls-15.html#jls-15.13.1">JLS section
   * 15.13.1</a> for a complete definition.
   *
   * <p>The type of a member reference is a functional interface. The function type of a member
   * reference is the type of the single abstract method declared by the functional interface. The
   * compile-time declaration type is the type of the actual method referenced by the method
   * reference, i.e. the method that is actually being referenced.
   *
   * <p>For example,
   *
   * <pre>{@code
   * static class MyClass {
   *   String field;
   *   public static int compareByField(MyClass a, MyClass b) { ... }
   * }
   * Comparator<MyClass> func = MyClass::compareByField;
   * }</pre>
   *
   * The function type is {@code compare(Comparator<MyClass> this, MyClass o1, MyClass o2)} where as
   * the compile-time declaration type is {@code compareByField(MyClass a, MyClass b)}.
   *
   * @param memRef method reference tree
   * @param targetType {@code memRef}'s target type
   * @return the compile-time declaration of the method reference
   */
  public InvocationType compileTimeDeclarationType(
      MemberReferenceTree memRef, AbstractType targetType) {
    // The type of the expression or type use, <expression>::method or <type use>::method.
    final ExpressionTree qualifierExpression = memRef.getQualifierExpression();
    final ReferenceKind memRefKind = ((JCMemberReference) memRef).kind;
    AnnotatedTypeMirror enclosingType;

    if (memRef.getMode() == ReferenceMode.NEW) {
      enclosingType = typeFactory.getAnnotatedTypeFromTypeTree(qualifierExpression);
      if (enclosingType.getKind() == TypeKind.DECLARED
          && ((AnnotatedDeclaredType) enclosingType).isUnderlyingTypeRaw()) {
        // The member reference is HashMap::new so the type arguments for HashMap must be
        // inferred.
        // So use the type declared type.
        TypeElement typeEle = TypesUtils.getTypeElement(enclosingType.getUnderlyingType());
        enclosingType = typeFactory.getAnnotatedType(typeEle);
      }
    } else if (memRefKind == ReferenceKind.UNBOUND) {
      enclosingType = typeFactory.getAnnotatedTypeFromTypeTree(qualifierExpression);
      if (enclosingType.getKind() == TypeKind.DECLARED
          && ((AnnotatedDeclaredType) enclosingType).isUnderlyingTypeRaw()) {
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
      enclosingType = AbstractType.makeGround((AnnotatedDeclaredType) enclosingType, typeFactory);
    }
    // The type of the compileTimeDeclaration if it were invoked with a receiver expression
    // of type {@code type}
    AnnotatedExecutableType compileTimeType =
        typeFactory.methodFromUse(memRef, compileTimeDeclaration, enclosingType).executableType;

    return new InvocationType(
        compileTimeType,
        TreeUtils.compileTimeDeclarationType(memRef, targetType.getJavaType(), context.env),
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

  /**
   * Returns the type of {@code element} using the type variable to inference variable mapping,
   * {@code map}.
   *
   * @param element some element
   * @param map type parameter to inference variables to use
   * @return the type of {@code element}
   */
  public AbstractType getTypeOfElement(Element element, Theta map) {
    AnnotatedTypeMirror atm = typeFactory.getAnnotatedType(element).asUse();
    return InferenceType.create(atm, element.asType(), map, context);
  }

  /**
   * Returns the type of {@code pEle} using the type variable to inference variable mapping, {@code
   * map}.
   *
   * @param pEle some element
   * @param map type parameter to inference variables to use
   * @return the type of {@code pEle}
   */
  public AbstractType getTypeOfBound(TypeParameterElement pEle, Theta map) {
    AnnotatedTypeVariable atm = (AnnotatedTypeVariable) typeFactory.getAnnotatedType(pEle);
    return InferenceType.create(
        atm.getUpperBound(), ((TypeVariable) pEle.asType()).getUpperBound(), map, context);
  }

  /**
   * Return the proper type for object.
   *
   * @return the proper type for object
   */
  public ProperType getObject() {
    TypeMirror objectTypeMirror =
        TypesUtils.typeFromClass(Object.class, context.modelTypes, context.env.getElementUtils());
    AnnotatedTypeMirror object =
        AnnotatedTypeMirror.createType(objectTypeMirror, typeFactory, false);
    object.addMissingAnnotations(typeFactory.getQualifierHierarchy().getTopAnnotations());
    return new ProperType(object, objectTypeMirror, context);
  }

  /**
   * Return the least upper bounds of {@code properTypes}.
   *
   * @param properTypes types to lub
   * @return the least upper bounds of {@code properTypes}
   */
  public ProperType lub(LinkedHashSet<ProperType> properTypes) {
    if (properTypes.isEmpty()) {
      return null;
    }
    TypeMirror tiTypeMirror = null;
    AnnotatedTypeMirror ti = null;
    for (ProperType liProperType : properTypes) {
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

  /**
   * Returns the greatest lower bound of {@code abstractTypes}.
   *
   * @param abstractTypes types to glb
   * @return the greatest upper bounds of {@code abstractTypes}
   */
  public AbstractType glb(LinkedHashSet<AbstractType> abstractTypes) {
    AbstractType ti = null;
    for (AbstractType liProperType : abstractTypes) {
      AbstractType li = liProperType;
      if (ti == null) {
        ti = li;
      } else {
        ti = glb(ti, li);
      }
    }
    return ti;
  }

  /**
   * Returns the greatest lower bound of {@code a} and {@code b}.
   *
   * @param a type to glb
   * @param b type to glb
   * @return the greatest lower bound of {@code a} and {@code b}
   */
  public AbstractType glb(AbstractType a, AbstractType b) {
    Type aJavaType = (Type) a.getJavaType();
    Type bJavaType = (Type) b.getJavaType();
    TypeMirror glb = TypesUtils.greatestLowerBound(aJavaType, bJavaType, context.env);

    AnnotatedTypeMirror aAtm = a.getAnnotatedType();
    AnnotatedTypeMirror bAtm = b.getAnnotatedType();
    AnnotatedTypeMirror glbATM = AnnotatedTypes.annotatedGLB(typeFactory, aAtm, bAtm);
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

  /**
   * Return the proper type for RuntimeException.
   *
   * @return the proper type for RuntimeException
   */
  public ProperType getRuntimeException() {
    AnnotatedTypeMirror runtimeEx =
        AnnotatedTypeMirror.createType(context.runtimeEx, typeFactory, false);
    runtimeEx.addMissingAnnotations(typeFactory.getQualifierHierarchy().getTopAnnotations());
    return new ProperType(runtimeEx, context.runtimeEx, context);
  }

  /**
   * Creates and returns the set of checked exception constraints for the given lambda or method
   * reference.
   *
   * @param expression a lambda or method reference expression
   * @param targetType the target type of {@code expression}
   * @param map theta
   * @return the set of checked exception constraints for the given lambda or method reference
   */
  public ConstraintSet getCheckedExceptionConstraints(
      ExpressionTree expression, AbstractType targetType, Theta map) {
    ConstraintSet constraintSet = new ConstraintSet();
    ExecutableElement ele = (ExecutableElement) TreeUtils.findFunction(expression, context.env);
    // The types in the function type's throws clause that are not proper types.
    List<Variable> es = new ArrayList<>();
    List<ProperType> properTypes = new ArrayList<>();

    AnnotatedExecutableType functionType =
        AnnotatedTypes.asMemberOf(
            context.modelTypes, context.typeFactory, targetType.getAnnotatedType(), ele);
    Iterator<AnnotatedTypeMirror> iter = functionType.getThrownTypes().iterator();
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
          CheckedExceptionsUtil.thrownCheckedExceptions((LambdaExpressionTree) expression, context);
      thrownTypes =
          CheckedExceptionsUtil.thrownCheckedExceptionsATM(
              (LambdaExpressionTree) expression, context);
    } else {
      thrownTypeMirrors =
          TypesUtils.findFunctionType(TreeUtils.typeOf(expression), context.env).getThrownTypes();
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
              new Typing(new ProperType(iter2.next(), xi, context), ei, Constraint.Kind.SUBTYPE));
          ei.getBounds().setHasThrowsBound(true);
        }
      }
    }

    return constraintSet;
  }

  /**
   * Creates a wildcard using the upper and lower bounds provided.
   *
   * @param lowerBound a proper type or null
   * @param upperBound an abstract type or null
   * @return a wildcard with the provided upper and lower bounds
   */
  public ProperType createWildcard(ProperType lowerBound, AbstractType upperBound) {
    TypeMirror wildcard =
        TypesUtils.createWildcard(
            lowerBound == null ? null : lowerBound.getJavaType(),
            upperBound == null ? null : upperBound.getJavaType(),
            context.env.getTypeUtils());
    AnnotatedWildcardType wildcardAtm =
        (AnnotatedWildcardType) AnnotatedTypeMirror.createType(wildcard, typeFactory, false);
    if (lowerBound != null) {
      wildcardAtm.setSuperBound(lowerBound.getAnnotatedType());
    }
    if (upperBound != null) {
      wildcardAtm.setExtendsBound(upperBound.getAnnotatedType());
    }
    return new ProperType(wildcardAtm, wildcard, context);
  }

  /**
   * Creates a fresh type variable using the upper and lower bounds provided.
   *
   * @param lowerBound a proper type or null
   * @param upperBound an abstract type or null
   * @return a fresh type variable with the provided upper and lower bounds
   */
  public ProperType createFreshTypeVariable(ProperType lowerBound, AbstractType upperBound) {
    TypeMirror freshTypeVariable =
        TypesUtils.freshTypeVariable(
            lowerBound == null ? null : lowerBound.getJavaType(),
            upperBound == null ? null : upperBound.getJavaType(),
            context.env);
    AnnotatedTypeVariable typeVariable =
        (AnnotatedTypeVariable)
            AnnotatedTypeMirror.createType(freshTypeVariable, typeFactory, false);
    if (lowerBound != null) {
      typeVariable.setLowerBound(lowerBound.getAnnotatedType());
    }
    if (upperBound != null) {
      typeVariable.setUpperBound(upperBound.getAnnotatedType());
    }
    return new ProperType(typeVariable, freshTypeVariable, context);
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
}
