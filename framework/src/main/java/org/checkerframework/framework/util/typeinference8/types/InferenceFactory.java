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
import com.sun.source.tree.SwitchExpressionTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import org.checkerframework.checker.interning.qual.Interned;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.framework.type.TypeVariableSubstitutor;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.framework.util.typeinference8.constraint.ConstraintSet;
import org.checkerframework.framework.util.typeinference8.constraint.TypeConstraint;
import org.checkerframework.framework.util.typeinference8.constraint.Typing;
import org.checkerframework.framework.util.typeinference8.util.CheckedExceptionsUtil;
import org.checkerframework.framework.util.typeinference8.util.CheckedExceptionsUtil.ThrownCheckedException;
import org.checkerframework.framework.util.typeinference8.util.Java8InferenceContext;
import org.checkerframework.framework.util.typeinference8.util.Theta;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.SwitchExpressionScanner;
import org.checkerframework.javacutil.SwitchExpressionScanner.FunctionalSwitchExpressionScanner;
import org.checkerframework.javacutil.TreePathUtil;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TreeUtils.MemberReferenceKind;
import org.checkerframework.javacutil.TypeAnnotationUtils;
import org.checkerframework.javacutil.TypesUtils;
import org.plumelib.util.IPair;

/** Factory that creates AbstractTypes. */
public class InferenceFactory {

  /** AnnotatedTypeFactory used to get annotated types. */
  private final AnnotatedTypeFactory typeFactory;

  /** Stores information about the current inference problem being solved. */
  private final Java8InferenceContext context;

  /**
   * Creates an inference factory.
   *
   * @param context the context
   */
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
    GenericAnnotatedTypeFactory<?, ?, ?, ?> factory =
        (GenericAnnotatedTypeFactory<?, ?, ?, ?>) context.typeFactory;
    TreePath path = context.getPathToExpression();
    Tree contextTree = TreePathUtil.getContextForPolyExpression(path);
    if (contextTree == null) {
      AnnotatedTypeMirror dummy = factory.getDummyAssignedTo((ExpressionTree) path.getLeaf());
      if (dummy == null || dummy.containsCapturedTypes()) {
        return null;
      }
      return new ProperType(dummy, this.context);
    }

    switch (contextTree.getKind()) {
      case ASSIGNMENT -> {
        ExpressionTree variable = ((AssignmentTree) contextTree).getVariable();
        AnnotatedTypeMirror atm = factory.getAnnotatedTypeLhs(variable);
        return new ProperType(atm, this.context);
      }
      case TYPE_CAST -> {
        Tree cast = ((TypeCastTree) contextTree).getType();
        AnnotatedTypeMirror castType = factory.getAnnotatedTypeFromTypeTree(cast);
        return new ProperType(castType, this.context);
      }
      case VARIABLE -> {
        AnnotatedTypeMirror variableAtm = assignedToVariable(factory, contextTree);
        return new ProperType(variableAtm, this.context);
      }
      case METHOD_INVOCATION -> {
        MethodInvocationTree methodInvocation = (MethodInvocationTree) contextTree;

        AnnotatedExecutableType executableType =
            factory.methodFromUseWithoutTypeArgInference(methodInvocation).executableType();

        AnnotatedTypeMirror paramType =
            assignedToExecutable(
                path, methodInvocation, methodInvocation.getArguments(), executableType);
        return new ProperType(paramType, this.context);
      }
      case NEW_CLASS -> {
        NewClassTree newClassTree = (NewClassTree) contextTree;
        AnnotatedExecutableType constructorType =
            factory.constructorFromUseWithoutTypeArgInference(newClassTree).executableType();
        AnnotatedTypeMirror constATM =
            assignedToExecutable(path, newClassTree, newClassTree.getArguments(), constructorType);
        return new ProperType(constATM, this.context);
      }
      case NEW_ARRAY -> {
        NewArrayTree newArrayTree = (NewArrayTree) contextTree;
        AnnotatedArrayType type = factory.getAnnotatedType(newArrayTree);
        AnnotatedTypeMirror component = type.getComponentType();
        return new ProperType(component, this.context);
      }
      case LAMBDA_EXPRESSION -> {
        LambdaExpressionTree lambdaTree = (LambdaExpressionTree) contextTree;
        AnnotatedExecutableType fninf = factory.getFunctionTypeFromTree(lambdaTree);
        AnnotatedTypeMirror res = fninf.getReturnType();
        if (res.getKind() == TypeKind.VOID) {
          return null;
        }
        return new ProperType(res, this.context);
      }
      case RETURN -> {
        Set<Kind> kinds = EnumSet.of(Tree.Kind.LAMBDA_EXPRESSION, Tree.Kind.METHOD);
        Tree enclosing = TreePathUtil.enclosingOfKind(path, kinds);
        if (enclosing instanceof MethodTree methodTree) {
          AnnotatedTypeMirror res = factory.getMethodReturnType(methodTree);
          return new ProperType(res, this.context);
        } else {
          LambdaExpressionTree lambdaTree = (LambdaExpressionTree) enclosing;
          AnnotatedExecutableType fninf = factory.getFunctionTypeFromTree(lambdaTree);
          AnnotatedTypeMirror res = fninf.getReturnType();
          return new ProperType(res, this.context);
        }
      }
      default -> {
        if (contextTree.getKind().asInterface() == CompoundAssignmentTree.class) {
          // 11 Tree kinds are compound assignments, so don't use it in the switch
          ExpressionTree var = ((CompoundAssignmentTree) contextTree).getVariable();
          AnnotatedTypeMirror res = factory.getAnnotatedTypeLhs(var);

          return new ProperType(res, this.context);
        } else {
          throw new BugInCF(
              "Unexpected assignment context.%nKind: %s%nTree: %s",
              contextTree.getKind(), contextTree);
        }
      }
    }
  }

  /**
   * If the variable's type is a type variable, return getAnnotatedTypeLhsNoTypeVarDefault(tree).
   * Rationale:
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
   * improve type argument inference in this case by using the lower bound of {@code S} instead of
   * the local variable default.
   *
   * @param atypeFactory AnnotatedTypeFactory
   * @param assignmentContext VariableTree
   * @return AnnotatedTypeMirror of Assignment context
   */
  public static AnnotatedTypeMirror assignedToVariable(
      AnnotatedTypeFactory atypeFactory, Tree assignmentContext) {
    if (atypeFactory instanceof GenericAnnotatedTypeFactory<?, ?, ?, ?> gatf) {
      return gatf.getAnnotatedTypeLhsNoTypeVarDefault(assignmentContext);
    } else {
      return atypeFactory.getAnnotatedType(assignmentContext);
    }
  }

  /**
   * Returns the rhs of the assignment of an argument and its formal parameter.
   *
   * @param path path to the argument
   * @param invocation a method or constructor invocation
   * @param arguments the argument expression tress
   * @param executableType the type of the method or constructor
   * @return the rhs of the assignment of an argument and its formal parameter
   */
  private static AnnotatedTypeMirror assignedToExecutable(
      TreePath path,
      ExpressionTree invocation,
      List<? extends ExpressionTree> arguments,
      AnnotatedExecutableType executableType) {
    int treeIndex = argumentIndex(path, invocation, arguments);

    if (treeIndex >= executableType.getParameterTypes().size() - 1
        && TreeUtils.isVarargsCall(invocation)) {
      treeIndex = executableType.getParameterTypes().size() - 1;
      AnnotatedTypeMirror typeMirror = executableType.getParameterTypes().get(treeIndex);
      return ((AnnotatedArrayType) typeMirror).getComponentType();
    }

    return executableType.getParameterTypes().get(treeIndex);
  }

  /**
   * Returns the index in {@code arguments} of the argument that contains the tree at the leaf of
   * {@code path}. Throws {@link BugInCF} if no argument contains that tree.
   *
   * @param path path to an expression whose target type is being computed
   * @param invocation a method or constructor invocation; used only in the error message
   * @param arguments the argument expression trees of {@code invocation}
   * @return the index in {@code arguments} of the argument that contains the tree at the leaf of
   *     {@code path}
   */
  private static int argumentIndex(
      TreePath path, ExpressionTree invocation, List<? extends ExpressionTree> arguments) {
    for (int i = 0; i < arguments.size(); ++i) {
      if (isArgument(path, arguments.get(i))) {
        return i;
      }
    }
    throw new BugInCF(
        "Not an argument of the invocation. tree: %s invocation: %s", path.getLeaf(), invocation);
  }

  /**
   * Returns true if argumentTree is the tree at the leaf of path. If tree is a conditional
   * expression, isArgument is called recursively on the true and false expressions. If tree is a
   * switch expression isArgument is called recursively on all yielded expressions.
   *
   * @param path tree path might contain {@code argumentTree}
   * @param argumentTree an expression tree that might be in {@code path}
   * @return true if argumentTree is the tree at the leaf of path
   */
  @SuppressWarnings("interning:not.interned") // Checking for exact object.
  private static boolean isArgument(TreePath path, ExpressionTree argumentTree) {
    argumentTree = TreeUtils.withoutParens(argumentTree);
    if (argumentTree == path.getLeaf()) {
      return true;
    } else if (argumentTree instanceof ConditionalExpressionTree conditionalExpressionTree) {
      return isArgument(path, conditionalExpressionTree.getTrueExpression())
          || isArgument(path, conditionalExpressionTree.getFalseExpression());
    } else if (argumentTree instanceof SwitchExpressionTree switchExpressionTree) {
      SwitchExpressionScanner<Boolean, Void> scanner =
          new FunctionalSwitchExpressionScanner<>(
              (tree, unused) -> isArgument(path, tree),
              (r1, r2) -> (r1 != null && r1) || (r2 != null && r2));
      return scanner.scanSwitchExpression(switchExpressionTree, null);
    }
    return false;
  }

  /**
   * Returns the type of the receiver of {@code tree} or null if {@code tree} does not have a
   * receiver.
   *
   * @param tree an expression tree
   * @return the type of the receiver of {@code tree} or null if {@code tree} does not have a
   *     receiver
   */
  private static @Nullable DeclaredType getReceiverType(ExpressionTree tree) {
    Tree receiverTree;
    if (tree instanceof NewClassTree nct) {
      receiverTree = nct.getEnclosingExpression();
      if (receiverTree == null && nct.getClassBody() == null) {
        TypeMirror t = TreeUtils.elementFromUse(nct).getReceiverType();
        if (t instanceof DeclaredType dt) {
          return dt;
        }
        return null;
      }
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
   * Returns ExecutableType of the method invocation or new class tree adapted to the call site.
   *
   * @param expressionTree a method invocation or new class tree
   * @param context the context
   * @return ExecutableType of the method invocation or new class tree adapted to the call site
   */
  public static ExecutableType getTypeOfMethodAdaptedToUse(
      ExpressionTree expressionTree, Java8InferenceContext context) {
    assert expressionTree instanceof NewClassTree || expressionTree instanceof MethodInvocationTree;

    ExecutableElement ele = (ExecutableElement) TreeUtils.elementFromUse(expressionTree);
    ExecutableType executableType = null;
    // First adapt to receiver
    if (!ElementUtils.isStatic(ele)) {
      DeclaredType receiverType = getReceiverType(expressionTree);
      if (receiverType == null && expressionTree instanceof MethodInvocationTree) {
        receiverType = context.enclosingType;
      } else if (receiverType != null) {
        receiverType = (DeclaredType) context.types.capture((Type) receiverType);
      }

      while (receiverType != null
          && context.types.asSuper((Type) receiverType, (Symbol) ele.getEnclosingElement())
              == null) {
        TypeMirror enclosing = receiverType.getEnclosingType();
        if (enclosing == null || enclosing.getKind() != TypeKind.DECLARED) {
          if (expressionTree instanceof NewClassTree) {
            // No receiver for the constructor.
            executableType = (ExecutableType) ele.asType();
            break;
          } else {
            throw new BugInCF("Method not found");
          }
        }
        receiverType = (DeclaredType) enclosing;
      }
      if (receiverType == null) {
        executableType = (ExecutableType) ele.asType();
      }
      if (executableType == null) {
        javax.lang.model.util.Types types = context.env.getTypeUtils();
        executableType = (ExecutableType) types.asMemberOf(receiverType, ele);
      }
    } else {
      executableType = (ExecutableType) TreeUtils.elementFromUse(expressionTree).asType();
    }

    // Adapt to class type arguments.
    if (expressionTree instanceof NewClassTree newClassTree
        && !TreeUtils.isDiamondTree(expressionTree)) {
      List<? extends Tree> typeArgs = TreeUtils.getTypeArgumentsToNewClassTree(newClassTree);
      if (!typeArgs.isEmpty()) {
        ExecutableElement e = TreeUtils.elementFromUse(newClassTree);
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
        executableType =
            (ExecutableType)
                TypesUtils.substitute(executableType, typeVariables, args, context.env);
      } else if (TypesUtils.isRaw(TreeUtils.typeOf(newClassTree))) {
        executableType = (ExecutableType) context.types.erasure((Type) executableType);
      }
    }
    // Adapt to explicit method type arguments.
    List<? extends Tree> typeArgs;
    if (expressionTree instanceof MethodInvocationTree mit) {
      typeArgs = mit.getTypeArguments();
    } else {
      typeArgs = ((NewClassTree) expressionTree).getTypeArguments();
    }
    if (typeArgs.isEmpty()) {
      return executableType;
    }
    List<? extends TypeParameterElement> typeParams = ele.getTypeParameters();
    List<TypeVariable> typeVariables = new ArrayList<>();
    for (TypeParameterElement typeParam : typeParams) {
      typeVariables.add((TypeVariable) typeParam.asType());
    }

    List<TypeMirror> args = new ArrayList<>();
    for (Tree arg : typeArgs) {
      args.add(TreeUtils.typeOf(arg));
    }

    return (ExecutableType) TypesUtils.substitute(executableType, typeVariables, args, context.env);
  }

  /**
   * Returns the least upper bound of {@code tm1} and {@code tm2}.
   *
   * @param processingEnv the processing environment
   * @param tm1 a type
   * @param tm2 a type
   * @return the least upper bound of {@code tm1} and {@code tm2}
   */
  public static TypeMirror lub(
      ProcessingEnvironment processingEnv, TypeMirror tm1, TypeMirror tm2) {
    Type t1 = TypeAnnotationUtils.unannotatedType(tm1);
    Type t2 = TypeAnnotationUtils.unannotatedType(tm2);
    JavacProcessingEnvironment javacEnv = (JavacProcessingEnvironment) processingEnv;
    Types types = Types.instance(javacEnv.getContext());

    return types.lub(t1, t2);
  }

  /**
   * Returns the greatest lower bound of {@code tm1} and {@code tm2}.
   *
   * @param processingEnv the processing environment
   * @param tm1 a type
   * @param tm2 a type
   * @return the greatest lower bound of {@code tm1} and {@code tm2}
   */
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
   * <p>Creates inference variables for the type parameters to {@code executableType} for a
   * particular {@code invocation}. Initializes the bounds of the variables. Returns a mapping from
   * type variables to newly created variables.
   *
   * <p>Otherwise, returns the previously created mapping.
   *
   * @param invocation method or constructor invocation
   * @param executableType type of generic method
   * @param context Java8InferenceContext
   * @return a mapping of the type variables of {@code executableType} to inference variables
   */
  public Theta createThetaForInvocation(
      ExpressionTree invocation,
      AbstractExecutableType executableType,
      Java8InferenceContext context) {
    if (context.maps.containsKey(invocation)) {
      return context.maps.get(invocation);
    }
    Theta map = new Theta();

    // Create inference variables for the type parameters to executableType

    for (AnnotatedTypeVariable pl : executableType.getAnnotatedTypeVariables()) {
      @SuppressWarnings("interning:interned.object.creation")
      Variable al = new @Interned Variable(pl, pl.getUnderlyingType(), invocation, context, map);
      map.put(pl.getUnderlyingType(), al);
    }
    if (TreeUtils.isDiamondTree(invocation)) {
      // If the invocation is a diamondTree, such as new List<>(...), then create variables
      // for the class type parameters, too.
      Element classEle =
          ElementUtils.enclosingTypeElement(TreeUtils.elementFromUse((NewClassTree) invocation));
      if (classEle.getSimpleName().contentEquals("")) {
        classEle =
            ((DeclaredType) TreeUtils.typeOf(((NewClassTree) invocation).getIdentifier()))
                .asElement();
      }
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
        @SuppressWarnings("interning:interned.object.creation")
        Variable al = new @Interned Variable(atv, pl, invocation, context, map);
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
      MemberReferenceTree memRef,
      CompileTimeDeclarationType compileTimeDecl,
      Java8InferenceContext context) {
    if (context.maps.containsKey(memRef)) {
      return context.maps.get(memRef);
    }

    Theta map = new Theta();
    TypeMirror preColonTreeType = TreeUtils.typeOf(memRef.getQualifierExpression());
    if (TreeUtils.isDiamondMemberReference(memRef)
        || TreeUtils.isLikeDiamondMemberReference(memRef)) {
      // If memRef is a constructor or method of a generic class whose type argument isn't
      // specified such as HashSet::new or HashSet::put
      // then add variables for the type arguments to the class.
      TypeElement classEle = (TypeElement) ((Type) preColonTreeType).asElement();
      DeclaredType classTypeMirror = (DeclaredType) classEle.asType();

      AnnotatedDeclaredType classType =
          (AnnotatedDeclaredType) typeFactory.getAnnotatedType(classTypeMirror.asElement());

      if (((Type) preColonTreeType).getTypeArguments().isEmpty()) {
        Iterator<AnnotatedTypeMirror> iter = classType.getTypeArguments().iterator();
        for (TypeMirror typeMirror : classTypeMirror.getTypeArguments()) {
          if (typeMirror.getKind() != TypeKind.TYPEVAR) {
            throw new BugInCF("Expected type variable, found: %s", typeMirror);
          }
          TypeVariable pl = (TypeVariable) typeMirror;
          AnnotatedTypeVariable atv = (AnnotatedTypeVariable) iter.next();
          @SuppressWarnings("interning:interned.object.creation")
          Variable al = new @Interned Variable(atv, pl, memRef, context, map);
          map.put(pl, al);
        }
      }
    }

    // Create inference variables for the type parameters to compileTimeDecl
    if (memRef.getTypeArguments() == null && compileTimeDecl.hasTypeVariables()) {
      Iterator<? extends AnnotatedTypeVariable> iter1 =
          compileTimeDecl.getAnnotatedTypeVariables().iterator();
      for (TypeVariable pl : compileTimeDecl.getTypeVariables()) {
        @SuppressWarnings("interning:interned.object.creation")
        Variable al = new @Interned Variable(iter1.next(), pl, memRef, context, map);
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
   * @return a mapping of the type variables of {@code functionalInterface} to inference variables
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
      @SuppressWarnings("interning:interned.object.creation")
      Variable ai = new @Interned Variable(atv, typeVar, lambda, context, map);
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
      @SuppressWarnings("interning:interned.object.creation")
      CaptureVariable al = new @Interned CaptureVariable(atv, pl, tree, context, map);
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
  public AbstractExecutableType getTypeOfMethodAdaptedToUse(ExpressionTree invocation) {
    AnnotatedExecutableType executableType;
    if (invocation instanceof MethodInvocationTree mit) {
      executableType = typeFactory.methodFromUseWithoutTypeArgInference(mit).executableType();
    } else {
      executableType =
          typeFactory
              .constructorFromUseWithoutTypeArgInference((NewClassTree) invocation)
              .executableType();
    }
    return new AbstractInvocationType(
        executableType, getTypeOfMethodAdaptedToUse(invocation, context), invocation, context);
  }

  /**
   * Returns the compile-time declaration of the method reference that is the method to which the
   * expression refers. See <a
   * href="https://docs.oracle.com/javase/specs/jls/se25/html/jls-15.html#jls-15.13.1">JLS section
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
   * <p>The function type is {@code compare(Comparator<MyClass> this, MyClass o1, MyClass o2)} where
   * as the compile-time declaration type is {@code compareByField(MyClass a, MyClass b)}.
   *
   * @param memRef method reference tree
   * @return the compile-time declaration of the method reference
   */
  public CompileTimeDeclarationType compileTimeDeclarationType(MemberReferenceTree memRef) {
    // The tree before :: is an expression or type use.
    final ExpressionTree preColonTree = memRef.getQualifierExpression();
    final MemberReferenceKind memRefKind = MemberReferenceKind.getMemberReferenceKind(memRef);
    AnnotatedTypeMirror enclosingType;

    if (memRef.getMode() == ReferenceMode.NEW) {
      enclosingType = typeFactory.getAnnotatedTypeFromTypeTree(preColonTree);
      if (TreeUtils.isDiamondMemberReference(memRef)) {
        // The member reference is HashMap::new so the type arguments for HashMap must be
        // inferred.
        // So use the type declared type.
        TypeElement typeEle = TypesUtils.getTypeElement(enclosingType.getUnderlyingType());
        enclosingType = typeFactory.getAnnotatedType(typeEle);
      }
    } else if (memRefKind == MemberReferenceKind.UNBOUND) {
      enclosingType = typeFactory.getAnnotatedTypeFromTypeTree(preColonTree);
      if (enclosingType.getKind() == TypeKind.DECLARED
          && ((AnnotatedDeclaredType) enclosingType).isUnderlyingTypeRaw()) {
        TypeElement typeEle = TypesUtils.getTypeElement(enclosingType.getUnderlyingType());
        enclosingType = typeFactory.getAnnotatedType(typeEle);
      }
    } else if (memRefKind == MemberReferenceKind.STATIC) {
      // The tree before :: is a type tree.
      enclosingType = typeFactory.getAnnotatedTypeFromTypeTree(preColonTree);
    } else { // memRefKind == MemberReferenceKind.BOUND
      // The tree before :: is an expression.
      enclosingType = typeFactory.getAnnotatedType(preColonTree);
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
        typeFactory
            .methodFromUseWithoutTypeArgInference(memRef, compileTimeDeclaration, enclosingType)
            .executableType();

    if (enclosingType.getKind() == TypeKind.DECLARED && memRefKind.isUnbound()) {
      // If compileTimeDeclaration is declared in a super class, then the receiver type is changed
      // to that super type. For method references, it should remain the given enclosing type.
      compileTimeType.setReceiverType((AnnotatedDeclaredType) enclosingType);
    }

    return new CompileTimeDeclarationType(
        compileTimeType, compileTimeType.getUnderlyingType(), memRef, enclosingType, context);
  }

  /**
   * Returns the pair of {@code a} as the least upper bound of {@code a} and {@code b} and {@code b}
   * as the least upper bound of {@code a} and {@code b}. Returns null if that least upper bound is
   * not a parameterized type or if either {@code a} or {@code b} has no supertype that is the same
   * class as that least upper bound.
   *
   * @param a type
   * @param b type
   * @return the pair of {@code a} as the least upper bound of {@code a} and {@code b} and {@code b}
   *     as the least upper bound of {@code a} and {@code b}, or null
   */
  public @Nullable IPair<AbstractType, AbstractType> getParameterizedSupers(
      AbstractType a, AbstractType b) {
    TypeMirror aTypeMirror = a.getJavaType();
    TypeMirror bTypeMirror = b.getJavaType();
    // com.sun.tools.javac.comp.Infer#getParameterizedSupers
    TypeMirror lubResult = lub(context.env, aTypeMirror, bTypeMirror);
    if (!TypesUtils.isParameterizedType(lubResult) || lubResult.getKind() == TypeKind.ARRAY) {
      return null;
    }

    Type asSuperOfA = context.types.asSuper((Type) aTypeMirror, ((Type) lubResult).asElement());
    Type asSuperOfB = context.types.asSuper((Type) bTypeMirror, ((Type) lubResult).asElement());
    if (asSuperOfA == null || asSuperOfB == null) {
      return null;
    }

    AbstractType aAsSuper = a.asSuper(asSuperOfA);
    AbstractType bAsSuper = b.asSuper(asSuperOfB);
    if (aAsSuper == null || bAsSuper == null) {
      return null;
    }

    return IPair.of(aAsSuper, bAsSuper);
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
    return InferenceType.create(atm, map, context);
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
    return InferenceType.create(atm.getUpperBound(), map, context);
  }

  /**
   * Returns the proper type for object.
   *
   * @return the proper type for object
   */
  public ProperType getObject() {
    TypeMirror objectTypeMirror =
        TypesUtils.typeFromClass(Object.class, context.modelTypes, context.env.getElementUtils());
    AnnotatedTypeMirror object =
        AnnotatedTypeMirror.createType(objectTypeMirror, typeFactory, false);
    object.addMissingAnnotations(typeFactory.getQualifierHierarchy().getTopAnnotations());
    return new ProperType(object, context);
  }

  /**
   * Returns the least upper bounds of {@code properTypes}, or null if {@code properTypes} is empty.
   *
   * @param properTypes types to lub
   * @return the least upper bounds of {@code properTypes}, or null
   */
  public @Nullable ProperType lub(Set<ProperType> properTypes) {
    if (properTypes.isEmpty()) {
      return null;
    }

    TypeMirror lubTM = null;
    AnnotatedTypeMirror lubATM = null;
    boolean ignoreAnnotations = false;
    for (ProperType properType : properTypes) {
      AnnotatedTypeMirror atm = properType.getAnnotatedType();
      TypeMirror tm = properType.getJavaType();
      if (lubATM == null) {
        lubATM = atm;
        lubTM = tm;
        ignoreAnnotations = properType.ignoreAnnotations;
      } else {
        lubTM = lub(context.env, lubTM, tm);
        if (properType.ignoreAnnotations == ignoreAnnotations) {
          lubATM = AnnotatedTypes.leastUpperBound(typeFactory, lubATM, atm, lubTM);
        } else if (properType.ignoreAnnotations) {
          // Only `lubATM`'s annotations are meaningful, so keep them.
          lubATM =
              AnnotatedTypes.asSuper(
                  typeFactory, lubATM, AnnotatedTypeMirror.createType(lubTM, typeFactory, false));
        } else {
          // Only `atm`'s annotations are meaningful, so keep them.
          lubATM =
              AnnotatedTypes.asSuper(
                  typeFactory, atm, AnnotatedTypeMirror.createType(lubTM, typeFactory, false));
        }
        // The annotations of a type that ignores annotations put no constraint on the result, so
        // the result ignores annotations only if every type does.  This is the same rule as in
        // `glb`.
        ignoreAnnotations = ignoreAnnotations && properType.ignoreAnnotations;
      }
    }
    return new ProperType(lubATM, context, ignoreAnnotations);
  }

  /**
   * Returns the greatest lower bound of {@code abstractTypes}, or null if {@code abstractTypes} is
   * empty.
   *
   * @param abstractTypes types to glb
   * @return the greatest lower bound of {@code abstractTypes}, or null
   */
  public @Nullable AbstractType glb(Set<AbstractType> abstractTypes) {
    AbstractType glb = null;
    for (AbstractType abstractType : abstractTypes) {
      glb = (glb == null) ? abstractType : glb(glb, abstractType);
    }
    return glb;
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
    if (glb.getKind() == TypeKind.ERROR) {
      // Javac cannot express the greatest lower bound; this happens for two type variables whose
      // bounds are mutually recursive.  AnnotatedTypes#annotatedGLB falls back to one of its
      // arguments, so use that same type here, to keep `glbATM` and `glb` consistent.
      glb = glbATM.getUnderlyingType();
    }
    if (a.ignoreAnnotations != b.ignoreAnnotations) {
      if (a.ignoreAnnotations) {
        glbATM.replaceAnnotations(bAtm.getPrimaryAnnotations());
      } else {
        glbATM.replaceAnnotations(aAtm.getPrimaryAnnotations());
      }
    }
    if (context.types.isSameType(aJavaType, (Type) glb)) {
      return a.create(glbATM, false);
    }

    if (context.types.isSameType(bJavaType, (Type) glb)) {
      return b.create(glbATM, false);
    }

    if (a.isInferenceType()) {
      return a.create(glbATM, false);
    } else if (b.isInferenceType()) {
      return b.create(glbATM, false);
    }

    assert a.isProper() && b.isProper();
    return new ProperType(glbATM, context, a.ignoreAnnotations && b.ignoreAnnotations);
  }

  /**
   * Returns the proper type for RuntimeException.
   *
   * @return the proper type for RuntimeException
   */
  public ProperType getRuntimeException() {
    AnnotatedTypeMirror runtimeException = typeFactory.getAnnotatedType(RuntimeException.class);
    runtimeException.addMissingAnnotations(typeFactory.getQualifierHierarchy().getTopAnnotations());
    return new ProperType(runtimeException, context);
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
    ExecutableElement ele = TreeUtils.findFunction(expression, context.env);
    // The types in the function type's throws clause that are not proper types.
    List<UseOfVariable> es = new ArrayList<>();
    List<ProperType> properTypes = new ArrayList<>();

    AnnotatedTypeMirror functionalInterface = targetType.getAnnotatedType();
    if (targetType.isWildcardParameterizedType()) {
      // JLS 9.9: the function type of a wildcard-parameterized functional interface type is the
      // function type of the type's non-wildcard parameterization.
      functionalInterface =
          AbstractType.makeGround((AnnotatedDeclaredType) functionalInterface, context.typeFactory);
    }
    AnnotatedExecutableType functionType =
        AnnotatedTypes.asMemberOf(
            context.modelTypes, context.typeFactory, functionalInterface, ele);

    for (AnnotatedTypeMirror thrownType : functionType.getThrownTypes()) {
      AbstractType ei = InferenceType.create(thrownType, map, context);
      // JLS 18.2.5 uses the proper types and the inference variables among the function type's
      // thrown types.  Every thrown type is one or the other, because no subclass of Throwable is
      // generic (JLS 8.1.2) and because the function type is that of a non-wildcard
      // parameterization; the `isUseOfVariable` test is defensive.
      if (ei.isProper()) {
        properTypes.add((ProperType) ei);
      } else if (ei.isUseOfVariable()) {
        UseOfVariable varEi = (UseOfVariable) ei;
        ProperType instantiation = varEi.getVariable().getInstantiation();
        if (instantiation != null) {
          properTypes.add(instantiation);
        } else {
          es.add(varEi);
        }
      }
    }
    if (es.isEmpty()) {
      return ConstraintSet.TRUE;
    }
    List<ThrownCheckedException> thrownExceptions;
    if (expression instanceof LambdaExpressionTree let) {
      thrownExceptions = CheckedExceptionsUtil.thrownCheckedExceptions(let, context);
    } else {
      List<? extends AnnotatedTypeMirror> thrownTypes =
          compileTimeDeclarationType((MemberReferenceTree) expression)
              .getAnnotatedType()
              .getThrownTypes();
      thrownExceptions = new ArrayList<>(thrownTypes.size());
      for (AnnotatedTypeMirror thrown : thrownTypes) {
        TypeMirror thrownJavaType = thrown.getUnderlyingType();
        if (CheckedExceptionsUtil.isCheckedException(thrownJavaType, context)) {
          thrownExceptions.add(new ThrownCheckedException(thrownJavaType, thrown));
        }
      }
    }

    for (ThrownCheckedException thrownException : thrownExceptions) {
      AnnotatedTypeMirror xiAnnotated = thrownException.annotatedType();
      boolean isSubtypeOfProper = false;
      for (ProperType properType : properTypes) {
        if (context
            .env
            .getTypeUtils()
            .isSubtype(xiAnnotated.getUnderlyingType(), properType.getJavaType())) {
          isSubtypeOfProper = true;
        }
      }
      if (!isSubtypeOfProper) {
        for (UseOfVariable ei : es) {
          constraintSet.add(
              new Typing(
                  "Exception constraint for " + expression,
                  new ProperType(xiAnnotated, context),
                  ei,
                  TypeConstraint.Kind.SUBTYPE));
          ei.setHasThrowsBound(true);
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
  public ProperType createWildcard(
      @Nullable ProperType lowerBound, @Nullable AbstractType upperBound) {
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
    return new ProperType(wildcardAtm, context);
  }

  /**
   * Creates a fresh type variable using the upper and lower bounds provided.
   *
   * @param lowerBound a proper type or null
   * @param lowerBoundAnnos annotations to use if {@code lowerBound} is null; a hierarchy that it
   *     does not mention gets the default qualifier for an implicit lower bound
   * @param upperBound an abstract type or null
   * @param upperBoundAnnos annotations to use if {@code upperBound} is null; a hierarchy that it
   *     does not mention gets the default qualifier for an implicit upper bound
   * @return a fresh type variable with the provided upper and lower bounds
   */
  public AbstractType createFreshTypeVariable(
      @Nullable ProperType lowerBound,
      Set<? extends AnnotationMirror> lowerBoundAnnos,
      @Nullable AbstractType upperBound,
      Set<? extends AnnotationMirror> upperBoundAnnos) {
    TypeMirror freshTypeVariable =
        TypesUtils.freshTypeVariable(
            upperBound == null ? null : upperBound.getJavaType(),
            lowerBound == null ? null : lowerBound.getJavaType(),
            context.env);
    AnnotatedTypeVariable typeVariable =
        (AnnotatedTypeVariable)
            AnnotatedTypeMirror.createType(freshTypeVariable, typeFactory, false);
    // Initialize bounds.
    typeVariable.getUpperBound();
    typeVariable.getLowerBound();
    if (lowerBound != null) {
      typeVariable.setLowerBound(lowerBound.getAnnotatedType());
    } else {
      typeVariable.getLowerBound().addAnnotations(lowerBoundAnnos);
    }
    if (upperBound != null) {
      typeVariable.setUpperBound(upperBound.getAnnotatedType());
    } else {
      typeVariable.getUpperBound().addAnnotations(upperBoundAnnos);
    }
    // Each bound of the fresh type variable must have an annotation in every hierarchy; otherwise a
    // later comparison against the bound crashes.  A bound that the caller did not supply is
    // `Object` or the null type, for which the caller's annotations might mention no hierarchy at
    // all, so fill in the defaults for the hierarchies that are still missing.
    typeFactory.addDefaultAnnotations(typeVariable);
    context.typeFactory.capturedTypeVarSubstitutor.substitute(
        typeVariable, Collections.singletonMap(typeVariable.getUnderlyingType(), typeVariable));
    AbstractType template = upperBound != null ? upperBound : lowerBound;
    if (template == null) {
      return new ProperType(typeVariable, context);
    }
    return template.create(typeVariable, false);
  }

  /**
   * Returns {@code typeArgs}, in which every use of a variable in {@code variables} has been
   * replaced by the corresponding element of {@code typeArgs}. An instantiation may mention another
   * variable that is being resolved at the same time, so the instantiations are not final until
   * this substitution has been performed.
   *
   * <p>{@code typeArgs} and {@code variables} are parallel lists: {@code typeArgs.get(i)} is the
   * instantiation of {@code variables.get(i)}.
   *
   * @param typeArgs the instantiations of {@code variables}
   * @param variables the variables that {@code typeArgs} instantiates
   * @return {@code typeArgs}, with uses of {@code variables} replaced by their instantiations
   */
  public List<AbstractType> getSubsTypeArgs(List<AbstractType> typeArgs, List<Variable> variables) {
    Map<TypeVariable, AnnotatedTypeMirror> map = new HashMap<>();
    for (int i = 0; i < typeArgs.size(); i++) {
      map.put(variables.get(i).getJavaType(), typeArgs.get(i).getAnnotatedType());
    }

    // TODO: If the instantiation of a variable mentions that same variable, then substitution does
    // not terminate the mention: the result still refers to the variable, because such a recursive
    // type cannot be written as a Java type.
    List<AbstractType> subsTypeArgs = new ArrayList<>();
    for (AbstractType typeArg : typeArgs) {
      AnnotatedTypeMirror type = typeArg.getAnnotatedType();
      TypeVariableSubstitutor typeVarSubstitutor = typeFactory.getTypeVarSubstitutor();
      AnnotatedTypeMirror subs;
      if (TypesUtils.isCapturedTypeVariable(type.getUnderlyingType())) {
        AnnotatedTypeVariable capTypeVar = (AnnotatedTypeVariable) type;
        AnnotatedTypeMirror upperBound =
            typeVarSubstitutor.substituteWithoutCopyingTypeArguments(
                map, capTypeVar.getUpperBound());
        AnnotatedTypeMirror lowerBound =
            typeVarSubstitutor.substituteWithoutCopyingTypeArguments(
                map, capTypeVar.getLowerBound());
        capTypeVar.setUpperBound(upperBound);
        capTypeVar.setLowerBound(lowerBound);
        subs = capTypeVar;
      } else {
        subs = typeVarSubstitutor.substituteWithoutCopyingTypeArguments(map, type);
      }
      subsTypeArgs.add(new ProperType(subs, context));
    }
    return subsTypeArgs;
  }
}
