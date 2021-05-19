package org.checkerframework.checker.lock;

import com.sun.source.tree.AnnotatedTypeTree;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.SynchronizedTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
import org.checkerframework.checker.lock.LockAnnotatedTypeFactory.SideEffectAnnotation;
import org.checkerframework.checker.lock.qual.EnsuresLockHeld;
import org.checkerframework.checker.lock.qual.EnsuresLockHeldIf;
import org.checkerframework.checker.lock.qual.GuardSatisfied;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.checkerframework.checker.lock.qual.GuardedByBottom;
import org.checkerframework.checker.lock.qual.GuardedByUnknown;
import org.checkerframework.checker.lock.qual.Holding;
import org.checkerframework.checker.lock.qual.LockHeld;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.dataflow.expression.Unknown;
import org.checkerframework.dataflow.qual.Deterministic;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.framework.flow.CFAbstractValue;
import org.checkerframework.framework.type.AnnotatedTypeFactory.ParameterizedExecutableType;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.framework.util.JavaExpressionParseUtil.JavaExpressionParseException;
import org.checkerframework.framework.util.StringToJavaExpression;
import org.checkerframework.framework.util.dependenttypes.DependentTypesError;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreePathUtil;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;
import org.plumelib.util.CollectionsPlume;

/**
 * The LockVisitor enforces the special type-checking rules described in the Lock Checker manual
 * chapter.
 *
 * @checker_framework.manual #lock-checker Lock Checker
 */
public class LockVisitor extends BaseTypeVisitor<LockAnnotatedTypeFactory> {
  private final Class<? extends Annotation> checkerGuardedByClass = GuardedBy.class;
  private final Class<? extends Annotation> checkerGuardSatisfiedClass = GuardSatisfied.class;

  protected static final Pattern SELF_RECEIVER_PATTERN = Pattern.compile("^<self>(\\.(.*))?$");

  public LockVisitor(BaseTypeChecker checker) {
    super(checker);
  }

  @Override
  public Void visitVariable(VariableTree node, Void p) { // visit a variable declaration
    // A user may not annotate a primitive type, a boxed primitive type or a String
    // with any qualifier from the @GuardedBy hierarchy.
    // They are immutable, so there is no need to guard them.

    TypeMirror tm = TreeUtils.typeOf(node);

    if (TypesUtils.isBoxedPrimitive(tm) || TypesUtils.isPrimitive(tm) || TypesUtils.isString(tm)) {
      AnnotatedTypeMirror atm = atypeFactory.getAnnotatedType(node);
      if (atm.hasExplicitAnnotationRelaxed(atypeFactory.GUARDSATISFIED)
          || atm.hasExplicitAnnotationRelaxed(atypeFactory.GUARDEDBY)
          || atm.hasExplicitAnnotation(atypeFactory.GUARDEDBYUNKNOWN)
          || atm.hasExplicitAnnotation(atypeFactory.GUARDEDBYBOTTOM)) {
        checker.reportError(node, "immutable.type.guardedby");
      }
    }

    issueErrorIfMoreThanOneGuardedByAnnotationPresent(node);

    return super.visitVariable(node, p);
  }

  /**
   * Issues an error if two or more of the following annotations are present on a variable
   * declaration.
   *
   * <ul>
   *   <li>{@code @org.checkerframework.checker.lock.qual.GuardedBy}
   *   <li>{@code @net.jcip.annotations.GuardedBy}
   *   <li>{@code @javax.annotation.concurrent.GuardedBy}
   * </ul>
   *
   * @param variableTree the VariableTree for the variable declaration used to determine if
   *     multiple @GuardedBy annotations are present and to report the error
   */
  private void issueErrorIfMoreThanOneGuardedByAnnotationPresent(VariableTree variableTree) {
    int guardedByAnnotationCount = 0;

    List<AnnotationMirror> annos =
        TreeUtils.annotationsFromTypeAnnotationTrees(variableTree.getModifiers().getAnnotations());
    for (AnnotationMirror anno : annos) {
      if (atypeFactory.areSameByClass(anno, GuardedBy.class)
          || AnnotationUtils.areSameByName(anno, "net.jcip.annotations.GuardedBy")
          || AnnotationUtils.areSameByName(anno, "javax.annotation.concurrent.GuardedBy")) {
        guardedByAnnotationCount++;
        if (guardedByAnnotationCount > 1) {
          checker.reportError(variableTree, "multiple.guardedby.annotations");
          return;
        }
      }
    }
  }

  @Override
  public LockAnnotatedTypeFactory createTypeFactory() {
    return new LockAnnotatedTypeFactory(checker);
  }

  /**
   * Issues an error if a method (explicitly or implicitly) annotated with @MayReleaseLocks has a
   * formal parameter or receiver (explicitly or implicitly) annotated with @GuardSatisfied. Also
   * issues an error if a synchronized method has a @LockingFree, @SideEffectFree, or @Pure
   * annotation.
   *
   * @param node the MethodTree of the method definition to visit
   */
  @Override
  public Void visitMethod(MethodTree node, Void p) {
    ExecutableElement methodElement = TreeUtils.elementFromDeclaration(node);

    issueErrorIfMoreThanOneLockPreconditionMethodAnnotationPresent(methodElement, node);

    SideEffectAnnotation sea = atypeFactory.methodSideEffectAnnotation(methodElement, true);

    if (sea == SideEffectAnnotation.MAYRELEASELOCKS) {
      boolean issueGSwithMRLWarning = false;

      VariableTree receiver = node.getReceiverParameter();
      if (receiver != null) {
        if (atypeFactory.getAnnotatedType(receiver).hasAnnotation(checkerGuardSatisfiedClass)) {
          issueGSwithMRLWarning = true;
        }
      }

      if (!issueGSwithMRLWarning) { // Skip loop if we already decided to issue the warning.
        for (VariableTree vt : node.getParameters()) {
          if (atypeFactory.getAnnotatedType(vt).hasAnnotation(checkerGuardSatisfiedClass)) {
            issueGSwithMRLWarning = true;
            break;
          }
        }
      }

      if (issueGSwithMRLWarning) {
        checker.reportError(node, "guardsatisfied.with.mayreleaselocks");
      }
    }

    // Issue an error if a non-constructor method definition has a return type of
    // @GuardSatisfied without an index.
    if (methodElement != null && methodElement.getKind() != ElementKind.CONSTRUCTOR) {
      AnnotatedTypeMirror returnTypeATM = atypeFactory.getAnnotatedType(node).getReturnType();

      if (returnTypeATM != null && returnTypeATM.hasAnnotation(GuardSatisfied.class)) {
        int returnGuardSatisfiedIndex = atypeFactory.getGuardSatisfiedIndex(returnTypeATM);

        if (returnGuardSatisfiedIndex == -1) {
          checker.reportError(node, "guardsatisfied.return.must.have.index");
        }
      }
    }

    if (!sea.isWeakerThan(SideEffectAnnotation.LOCKINGFREE)
        && methodElement.getModifiers().contains(Modifier.SYNCHRONIZED)) {
      checker.reportError(node, "lockingfree.synchronized.method", sea);
    }

    return super.visitMethod(node, p);
  }

  /**
   * Issues an error if two or more of the following annotations are present on a method.
   *
   * <ul>
   *   <li>{@code @Holding}
   *   <li>{@code @net.jcip.annotations.GuardedBy}
   *   <li>{@code @javax.annotation.concurrent.GuardedBy}
   * </ul>
   *
   * @param methodElement the ExecutableElement for the method call referred to by {@code node}
   * @param treeForErrorReporting the MethodTree used to report the error
   */
  private void issueErrorIfMoreThanOneLockPreconditionMethodAnnotationPresent(
      ExecutableElement methodElement, MethodTree treeForErrorReporting) {
    int lockPreconditionAnnotationCount = 0;

    if (atypeFactory.getDeclAnnotation(methodElement, Holding.class) != null) {
      lockPreconditionAnnotationCount++;
    }

    try {
      if (atypeFactory.jcipGuardedBy != null
          && atypeFactory.getDeclAnnotation(methodElement, atypeFactory.jcipGuardedBy) != null) {
        lockPreconditionAnnotationCount++;
      }

      if (lockPreconditionAnnotationCount < 2
          && atypeFactory.javaxGuardedBy != null
          && atypeFactory.getDeclAnnotation(methodElement, atypeFactory.javaxGuardedBy) != null) {
        lockPreconditionAnnotationCount++;
      }
    } catch (Exception e) {
      // Ignore exceptions from Class.forName
    }

    if (lockPreconditionAnnotationCount > 1) {
      checker.reportError(treeForErrorReporting, "multiple.lock.precondition.annotations");
    }
  }

  /**
   * When visiting a method call, if the receiver formal parameter has type @GuardSatisfied and the
   * receiver actual parameter has type @GuardedBy(...), this method verifies that the guard is
   * satisfied, and it returns true, indicating that the receiver subtype check should be skipped.
   * If the receiver actual parameter has type @GuardSatisfied, this method simply returns true
   * without performing any other actions. The method returns false otherwise.
   *
   * @param methodInvocationTree the MethodInvocationTree of the method being called
   * @param methodDefinitionReceiver the ATM of the formal receiver parameter of the method being
   *     called
   * @param methodCallReceiver the ATM of the receiver argument of the method call
   * @return whether the caller can skip the receiver subtype check
   */
  @Override
  protected boolean skipReceiverSubtypeCheck(
      MethodInvocationTree methodInvocationTree,
      AnnotatedTypeMirror methodDefinitionReceiver,
      AnnotatedTypeMirror methodCallReceiver) {

    AnnotationMirror primaryGb =
        methodCallReceiver.getAnnotationInHierarchy(atypeFactory.GUARDEDBYUNKNOWN);
    AnnotationMirror effectiveGb =
        methodCallReceiver.getEffectiveAnnotationInHierarchy(atypeFactory.GUARDEDBYUNKNOWN);

    // If the receiver actual parameter has type @GuardSatisfied, skip the subtype check.  Consider
    // only a @GuardSatisfied primary annotation - hence use primaryGb instead of effectiveGb.
    if (primaryGb != null && atypeFactory.areSameByClass(primaryGb, checkerGuardSatisfiedClass)) {
      AnnotationMirror primaryGbOnMethodDefinition =
          methodDefinitionReceiver.getAnnotationInHierarchy(atypeFactory.GUARDEDBYUNKNOWN);
      if (primaryGbOnMethodDefinition != null
          && atypeFactory.areSameByClass(primaryGbOnMethodDefinition, checkerGuardSatisfiedClass)) {
        return true;
      }
    }

    if (atypeFactory.areSameByClass(effectiveGb, checkerGuardedByClass)) {
      Set<AnnotationMirror> annos = methodDefinitionReceiver.getAnnotations();
      AnnotationMirror guardSatisfied =
          atypeFactory.getAnnotationByClass(annos, checkerGuardSatisfiedClass);
      if (guardSatisfied != null) {
        ExpressionTree receiverTree = TreeUtils.getReceiverTree(methodInvocationTree);
        if (receiverTree == null) {
          checkLockOfImplicitThis(methodInvocationTree, effectiveGb);
        } else {
          checkLock(receiverTree, effectiveGb);
        }
        return true;
      }
    }

    return false;
  }

  @Override
  protected Set<? extends AnnotationMirror> getExceptionParameterLowerBoundAnnotations() {
    Set<? extends AnnotationMirror> tops = atypeFactory.getQualifierHierarchy().getTopAnnotations();
    Set<AnnotationMirror> annotationSet = AnnotationUtils.createAnnotationSet();
    for (AnnotationMirror anno : tops) {
      if (AnnotationUtils.areSame(anno, atypeFactory.GUARDEDBYUNKNOWN)) {
        annotationSet.add(atypeFactory.GUARDEDBY);
      } else {
        annotationSet.add(anno);
      }
    }
    return annotationSet;
  }

  @Override
  protected void checkConstructorResult(
      AnnotatedExecutableType constructorType, ExecutableElement constructorElement) {
    // Newly created objects are guarded by nothing, so allow @GuardBy({}) on constructor results.
    AnnotationMirror anno =
        constructorType.getReturnType().getAnnotationInHierarchy(atypeFactory.GUARDEDBYUNKNOWN);
    if (!AnnotationUtils.areSame(anno, atypeFactory.GUARDEDBY)) {
      super.checkConstructorResult(constructorType, constructorElement);
    }
  }

  @Override
  protected void commonAssignmentCheck(
      AnnotatedTypeMirror varType,
      AnnotatedTypeMirror valueType,
      Tree valueTree,
      @CompilerMessageKey String errorKey,
      Object... extraArgs) {

    Kind valueTreeKind = valueTree.getKind();

    switch (valueTreeKind) {
      case NEW_CLASS:
      case NEW_ARRAY:
        // Avoid issuing warnings for: @GuardedBy(<something>) Object o = new Object();
        // Do NOT do this if the LHS is @GuardedByBottom.
        if (!varType.hasAnnotation(GuardedByBottom.class)) {
          return;
        }
        break;
      case INT_LITERAL:
      case LONG_LITERAL:
      case FLOAT_LITERAL:
      case DOUBLE_LITERAL:
      case BOOLEAN_LITERAL:
      case CHAR_LITERAL:
      case STRING_LITERAL:
        // Avoid issuing warnings for: @GuardedBy(<something>) Object o; o = <some literal>;
        // Do NOT do this if the LHS is @GuardedByBottom.
        if (!varType.hasAnnotation(GuardedByBottom.class)) {
          return;
        }
        break;
      default:
    }

    // In cases where assigning a value with a @GuardedBy annotation to a variable with a
    // @GuardSatisfied annotation is legal, this is our last chance to check that the appropriate
    // locks are held before the information in the @GuardedBy annotation is lost in the assignment
    // to the variable annotated with @GuardSatisfied. See the discussion of @GuardSatisfied in the
    // "Type-checking rules" section of the Lock Checker manual chapter for more details.

    if (varType.hasAnnotation(GuardSatisfied.class)) {
      if (valueType.hasAnnotation(GuardedBy.class)) {
        checkLock(valueTree, valueType.getAnnotation(GuardedBy.class));
        return;
      } else if (valueType.hasAnnotation(GuardSatisfied.class)) {
        // TODO: Find a cleaner, non-abstraction-breaking way to know whether method actual
        // parameters are being assigned to formal parameters.

        if (!errorKey.equals("argument")) {
          // If both @GuardSatisfied have no index, the assignment is not allowed because
          // the LHS and RHS expressions may be guarded by different lock expressions.
          // The assignment is allowed when matching a formal parameter to an actual
          // parameter (see the if block above).

          int varTypeGuardSatisfiedIndex = atypeFactory.getGuardSatisfiedIndex(varType);
          int valueTypeGuardSatisfiedIndex = atypeFactory.getGuardSatisfiedIndex(valueType);

          if (varTypeGuardSatisfiedIndex == -1 && valueTypeGuardSatisfiedIndex == -1) {
            checker.reportError(
                valueTree, "guardsatisfied.assignment.disallowed", varType, valueType);
          }
        } else {
          // The RHS can be @GuardSatisfied with a different index when matching method
          // formal parameters to actual parameters.
          // The actual matching is done in LockVisitor.visitMethodInvocation and a
          // guardsatisfied.parameters.must.match error
          // is issued if the parameters do not match exactly.
          // Do nothing here, since there is no precondition to be checked on a
          // @GuardSatisfied parameter.
          // Note: this matching of a @GS(index) to a @GS(differentIndex) is *only*
          // allowed when matching method formal parameters to actual parameters.

          return;
        }
      } else if (!atypeFactory.getTypeHierarchy().isSubtype(valueType, varType)) {
        // Special case: replace the @GuardSatisfied primary annotation on the LHS with
        // @GuardedBy({}) and see if it type checks.

        AnnotatedTypeMirror varType2 = varType.deepCopy(); // TODO: Would shallowCopy be sufficient?
        varType2.replaceAnnotation(atypeFactory.GUARDEDBY);
        if (atypeFactory.getTypeHierarchy().isSubtype(valueType, varType2)) {
          return;
        }
      }
    }

    super.commonAssignmentCheck(varType, valueType, valueTree, errorKey, extraArgs);
  }

  @Override
  public Void visitMemberSelect(MemberSelectTree tree, Void p) {
    if (TreeUtils.isFieldAccess(tree)) {
      AnnotatedTypeMirror atmOfReceiver = atypeFactory.getAnnotatedType(tree.getExpression());
      // The atmOfReceiver for "void.class" is TypeKind.VOID, which isn't annotated so avoid it.
      if (atmOfReceiver.getKind() != TypeKind.VOID) {
        AnnotationMirror gb =
            atmOfReceiver.getEffectiveAnnotationInHierarchy(atypeFactory.GUARDEDBYUNKNOWN);
        checkLock(tree.getExpression(), gb);
      }
    }

    return super.visitMemberSelect(tree, p);
  }

  private void reportFailure(
      @CompilerMessageKey String messageKey,
      MethodTree overriderTree,
      AnnotatedDeclaredType enclosingType,
      AnnotatedExecutableType overridden,
      AnnotatedDeclaredType overriddenType,
      List<String> overriderLocks,
      List<String> overriddenLocks) {
    // Get the type of the overriding method.
    AnnotatedExecutableType overrider = atypeFactory.getAnnotatedType(overriderTree);

    if (overrider.getTypeVariables().isEmpty() && !overridden.getTypeVariables().isEmpty()) {
      overridden = overridden.getErased();
    }
    String overriderMeth = overrider.toString();
    String overriderTyp = enclosingType.getUnderlyingType().asElement().toString();
    String overriddenMeth = overridden.toString();
    String overriddenTyp = overriddenType.getUnderlyingType().asElement().toString();

    if (overriderLocks == null || overriddenLocks == null) {
      checker.reportError(
          overriderTree, messageKey, overriderTyp, overriderMeth, overriddenTyp, overriddenMeth);
    } else {
      checker.reportError(
          overriderTree,
          messageKey,
          overriderTyp,
          overriderMeth,
          overriddenTyp,
          overriddenMeth,
          overriderLocks,
          overriddenLocks);
    }
  }

  /**
   * Ensures that subclass methods are annotated with a stronger or equally strong side effect
   * annotation than the parent class method.
   */
  @Override
  protected boolean checkOverride(
      MethodTree overriderTree,
      AnnotatedDeclaredType enclosingType,
      AnnotatedExecutableType overriddenMethodType,
      AnnotatedDeclaredType overriddenType) {

    boolean isValid = true;

    SideEffectAnnotation seaOfOverriderMethod =
        atypeFactory.methodSideEffectAnnotation(
            TreeUtils.elementFromDeclaration(overriderTree), false);
    SideEffectAnnotation seaOfOverridenMethod =
        atypeFactory.methodSideEffectAnnotation(overriddenMethodType.getElement(), false);

    if (seaOfOverriderMethod.isWeakerThan(seaOfOverridenMethod)) {
      isValid = false;
      reportFailure(
          "override.sideeffect",
          overriderTree,
          enclosingType,
          overriddenMethodType,
          overriddenType,
          null,
          null);
    }

    return super.checkOverride(overriderTree, enclosingType, overriddenMethodType, overriddenType)
        && isValid;
  }

  @Override
  public Void visitArrayAccess(ArrayAccessTree tree, Void p) {
    AnnotatedTypeMirror atmOfReceiver = atypeFactory.getAnnotatedType(tree.getExpression());
    AnnotationMirror gb =
        atmOfReceiver.getEffectiveAnnotationInHierarchy(atypeFactory.GUARDEDBYUNKNOWN);
    checkLock(tree.getExpression(), gb);
    return super.visitArrayAccess(tree, p);
  }

  /**
   * Skips the call to super and returns true.
   *
   * <p>{@code GuardedBy({})} is the default type on class declarations, which is a subtype of the
   * top annotation {@code @GuardedByUnknown}. However, it is valid to declare an instance of a
   * class with any annotation from the {@code @GuardedBy} hierarchy. Hence, this method returns
   * true for annotations in the {@code @GuardedBy} hierarchy.
   *
   * <p>Also returns true for annotations in the {@code @LockPossiblyHeld} hierarchy since the
   * default for that hierarchy is the top type and annotations from that hierarchy cannot be
   * explicitly written in code.
   */
  @Override
  public boolean isValidUse(
      AnnotatedDeclaredType declarationType, AnnotatedDeclaredType useType, Tree tree) {
    return true;
  }

  /**
   * When visiting a method invocation, issue an error if the side effect annotation on the called
   * method causes the side effect guarantee of the enclosing method to be violated. For example, a
   * method annotated with @ReleasesNoLocks may not call a method annotated with @MayReleaseLocks.
   * Also check that matching @GuardSatisfied(index) on a method's formal receiver/parameters
   * matches those in corresponding locations on the method call site.
   *
   * @param node the MethodInvocationTree of the method call being visited
   */
  @Override
  public Void visitMethodInvocation(MethodInvocationTree node, Void p) {
    ExecutableElement methodElement = TreeUtils.elementFromUse(node);

    SideEffectAnnotation seaOfInvokedMethod =
        atypeFactory.methodSideEffectAnnotation(methodElement, false);

    MethodTree enclosingMethod = TreePathUtil.enclosingMethod(atypeFactory.getPath(node));

    ExecutableElement enclosingMethodElement = null;
    if (enclosingMethod != null) {
      enclosingMethodElement = TreeUtils.elementFromDeclaration(enclosingMethod);
    }

    if (enclosingMethodElement != null) {
      SideEffectAnnotation seaOfContainingMethod =
          atypeFactory.methodSideEffectAnnotation(enclosingMethodElement, false);

      if (seaOfInvokedMethod.isWeakerThan(seaOfContainingMethod)) {
        checker.reportError(
            node,
            "method.guarantee.violated",
            seaOfContainingMethod.getNameOfSideEffectAnnotation(),
            enclosingMethodElement.getSimpleName(),
            methodElement.getSimpleName(),
            seaOfInvokedMethod.getNameOfSideEffectAnnotation());
      }
    }

    if (methodElement != null) {
      // Handle releasing of explicit locks. Verify that the lock expression is effectively final.
      ExpressionTree receiverTree = TreeUtils.getReceiverTree(node);

      ensureReceiverOfExplicitUnlockCallIsEffectivelyFinal(methodElement, receiverTree);

      // Handle acquiring of explicit locks. Verify that the lock expression is effectively final.

      // If the method causes expression "this" or "#1" to be locked, verify that those expressions
      // are effectively final.  TODO: generalize to any expression. This is currently designed only
      // to support methods in ReentrantLock and ReentrantReadWriteLock (which use the "this"
      // expression), as well as Thread.holdsLock (which uses the "#1" expression).

      AnnotationMirror ensuresLockHeldAnno =
          atypeFactory.getDeclAnnotation(methodElement, EnsuresLockHeld.class);

      List<String> expressions = new ArrayList<>();
      if (ensuresLockHeldAnno != null) {
        expressions.addAll(
            AnnotationUtils.getElementValueArray(
                ensuresLockHeldAnno, atypeFactory.ensuresLockHeldValueElement, String.class));
      }

      AnnotationMirror ensuresLockHeldIfAnno =
          atypeFactory.getDeclAnnotation(methodElement, EnsuresLockHeldIf.class);

      if (ensuresLockHeldIfAnno != null) {
        expressions.addAll(
            AnnotationUtils.getElementValueArray(
                ensuresLockHeldIfAnno,
                atypeFactory.ensuresLockHeldIfExpressionElement,
                String.class));
      }

      for (String expr : expressions) {
        if (expr.equals("this")) {
          // receiverTree will be null for implicit this, or class name receivers. But they are also
          // final. So nothing to be checked for them.
          if (receiverTree != null) {
            ensureExpressionIsEffectivelyFinal(receiverTree);
          }
        } else if (expr.equals("#1")) {
          ExpressionTree firstParameter = node.getArguments().get(0);
          if (firstParameter != null) {
            ensureExpressionIsEffectivelyFinal(firstParameter);
          }
        }
      }
    }

    // Check that matching @GuardSatisfied(index) on a method's formal receiver/parameters
    // matches those in corresponding locations on the method call site.

    ParameterizedExecutableType mType = atypeFactory.methodFromUse(node);
    AnnotatedExecutableType invokedMethod = mType.executableType;

    List<AnnotatedTypeMirror> requiredArgs =
        AnnotatedTypes.expandVarArgs(atypeFactory, invokedMethod, node.getArguments());

    // Index on @GuardSatisfied at each location. -1 when no @GuardSatisfied annotation was present.
    // Note that @GuardSatisfied with no index is normally represented as having index -1.
    // We would like to ignore a @GuardSatisfied with no index for these purposes, so if it is
    // encountered we leave its index as -1.
    // The first element of the array is reserved for the receiver.
    int guardSatisfiedIndex[] =
        new int[requiredArgs.size() + 1]; // + 1 for the receiver parameter type

    // Retrieve receiver types from method definition and method call

    guardSatisfiedIndex[0] = -1;

    AnnotatedTypeMirror methodDefinitionReceiver = null;
    AnnotatedTypeMirror methodCallReceiver = null;

    ExecutableElement invokedMethodElement = invokedMethod.getElement();
    if (!ElementUtils.isStatic(invokedMethodElement)
        && invokedMethod.getElement().getKind() != ElementKind.CONSTRUCTOR) {
      methodDefinitionReceiver = invokedMethod.getReceiverType();
      if (methodDefinitionReceiver != null
          && methodDefinitionReceiver.hasAnnotation(checkerGuardSatisfiedClass)) {
        guardSatisfiedIndex[0] = atypeFactory.getGuardSatisfiedIndex(methodDefinitionReceiver);
        methodCallReceiver = atypeFactory.getReceiverType(node);
      }
    }

    // Retrieve formal parameter types from the method definition

    for (int i = 0; i < requiredArgs.size(); i++) {
      guardSatisfiedIndex[i + 1] = -1;

      AnnotatedTypeMirror arg = requiredArgs.get(i);

      if (arg.hasAnnotation(checkerGuardSatisfiedClass)) {
        guardSatisfiedIndex[i + 1] = atypeFactory.getGuardSatisfiedIndex(arg);
      }
    }

    // Combine all of the actual parameters into one list of AnnotationMirrors

    ArrayList<AnnotationMirror> passedArgAnnotations = new ArrayList<>(guardSatisfiedIndex.length);
    passedArgAnnotations.add(
        methodCallReceiver == null
            ? null
            : methodCallReceiver.getAnnotationInHierarchy(atypeFactory.GUARDEDBYUNKNOWN));
    for (ExpressionTree tree : node.getArguments()) {
      passedArgAnnotations.add(
          atypeFactory
              .getAnnotatedType(tree)
              .getAnnotationInHierarchy(atypeFactory.GUARDEDBYUNKNOWN));
    }

    // Perform the validity check and issue an error if not valid.

    for (int i = 0; i < guardSatisfiedIndex.length; i++) {
      if (guardSatisfiedIndex[i] != -1) {
        for (int j = i + 1; j < guardSatisfiedIndex.length; j++) {
          if (guardSatisfiedIndex[i] == guardSatisfiedIndex[j]) {
            // The @GuardedBy/@GuardSatisfied/@GuardedByUnknown/@GuardedByBottom
            // annotations must be identical on the corresponding actual parameters.
            AnnotationMirror arg1Anno = passedArgAnnotations.get(i);
            AnnotationMirror arg2Anno = passedArgAnnotations.get(j);
            if (arg1Anno != null && arg2Anno != null) {
              boolean bothAreGSwithNoIndex = false;

              if (atypeFactory.areSameByClass(arg1Anno, checkerGuardSatisfiedClass)
                  && atypeFactory.areSameByClass(arg2Anno, checkerGuardSatisfiedClass)) {
                if (atypeFactory.getGuardSatisfiedIndex(arg1Anno) == -1
                    && atypeFactory.getGuardSatisfiedIndex(arg2Anno) == -1) {
                  // Generally speaking, two @GuardSatisfied annotations with no
                  // index are incomparable.
                  // TODO: If they come from the same variable, they are
                  // comparable.  Fix and add a test case.
                  bothAreGSwithNoIndex = true;
                }
              }

              if (bothAreGSwithNoIndex
                  || !(atypeFactory.getQualifierHierarchy().isSubtype(arg1Anno, arg2Anno)
                      || atypeFactory.getQualifierHierarchy().isSubtype(arg2Anno, arg1Anno))) {
                // TODO: allow these strings to be localized

                String formalParam1 = null;

                if (i == 0) {
                  formalParam1 = "The receiver type";
                } else {
                  formalParam1 = "Parameter #" + i; // i, not i-1, so the index is 1-based
                }

                String formalParam2 = "parameter #" + j; // j, not j-1, so the index is 1-based

                checker.reportError(
                    node,
                    "guardsatisfied.parameters.must.match",
                    formalParam1,
                    formalParam2,
                    invokedMethod.toString(),
                    guardSatisfiedIndex[i],
                    arg1Anno,
                    arg2Anno);
              }
            }
          }
        }
      }
    }

    return super.visitMethodInvocation(node, p);
  }

  /**
   * Issues an error if the receiver of an unlock() call is not effectively final.
   *
   * @param methodElement the ExecutableElement for a method call to unlock()
   * @param lockExpression the receiver tree for the method call to unlock(). Can be null.
   */
  private void ensureReceiverOfExplicitUnlockCallIsEffectivelyFinal(
      ExecutableElement methodElement, ExpressionTree lockExpression) {
    if (lockExpression == null) {
      // Implicit this, or class name receivers, are null. But they are also final. So nothing
      // to be checked for them.
      return;
    }

    if (!methodElement.getSimpleName().contentEquals("unlock")) {
      return;
    }

    TypeMirror lockExpressionType = TreeUtils.typeOf(lockExpression);

    ProcessingEnvironment processingEnvironment = checker.getProcessingEnvironment();

    javax.lang.model.util.Types types = processingEnvironment.getTypeUtils();

    // TODO: make a type declaration annotation for this rather than looking for the
    // Lock.unlock() method explicitly.
    TypeMirror lockInterfaceTypeMirror =
        TypesUtils.typeFromClass(Lock.class, types, processingEnvironment.getElementUtils());

    if (types.isSubtype(types.erasure(lockExpressionType), lockInterfaceTypeMirror)) {
      ensureExpressionIsEffectivelyFinal(lockExpression);
    }
  }

  /**
   * When visiting a synchronized block, issue an error if the expression has a type that implements
   * the java.util.concurrent.locks.Lock interface. This prevents explicit locks from being
   * accidentally used as built-in (monitor) locks. This is important because the Lock Checker does
   * not have a mechanism to separately keep track of the explicit lock and the monitor lock of an
   * expression that implements the Lock interface (i.e. there is a @LockHeld annotation used in
   * dataflow, but there are not distinct @MonitorLockHeld and @ExplicitLockHeld annotations). It is
   * assumed that both kinds of locks will never be held for any expression that implements Lock.
   *
   * <p>Additionally, a synchronized block may not be present in a method that has a @LockingFree
   * guarantee or stronger. An error is issued in this case.
   *
   * @param node the SynchronizedTree for the synchronized block being visited
   */
  @Override
  public Void visitSynchronized(SynchronizedTree node, Void p) {
    ProcessingEnvironment processingEnvironment = checker.getProcessingEnvironment();

    javax.lang.model.util.Types types = processingEnvironment.getTypeUtils();

    // TODO: make a type declaration annotation for this rather than looking for Lock.class
    // explicitly.
    TypeMirror lockInterfaceTypeMirror =
        TypesUtils.typeFromClass(Lock.class, types, processingEnvironment.getElementUtils());

    ExpressionTree synchronizedExpression = node.getExpression();

    ensureExpressionIsEffectivelyFinal(synchronizedExpression);

    TypeMirror expressionType =
        types.erasure(atypeFactory.getAnnotatedType(synchronizedExpression).getUnderlyingType());

    if (types.isSubtype(expressionType, lockInterfaceTypeMirror)) {
      checker.reportError(node, "explicit.lock.synchronized");
    }

    MethodTree enclosingMethod = TreePathUtil.enclosingMethod(atypeFactory.getPath(node));

    ExecutableElement methodElement = null;
    if (enclosingMethod != null) {
      methodElement = TreeUtils.elementFromDeclaration(enclosingMethod);

      SideEffectAnnotation seaOfContainingMethod =
          atypeFactory.methodSideEffectAnnotation(methodElement, false);

      if (!seaOfContainingMethod.isWeakerThan(SideEffectAnnotation.LOCKINGFREE)) {
        checker.reportError(
            node, "synchronized.block.in.lockingfree.method", seaOfContainingMethod);
      }
    }

    return super.visitSynchronized(node, p);
  }

  /**
   * Ensures that each variable accessed in an expression is final or effectively final and that
   * each called method in the expression is @Deterministic. Issues an error otherwise. Recursively
   * performs this check on method arguments. Only intended to be used on the expression of a
   * synchronized block.
   *
   * <p>Example: given the expression var1.field1.method1(var2.method2()).field2, var1, var2, field1
   * and field2 are enforced to be final or effectively final, and method1 and method2 are enforced
   * to be @Deterministic.
   *
   * @param lockExpressionTree the expression tree of a synchronized block
   */
  private void ensureExpressionIsEffectivelyFinal(final ExpressionTree lockExpressionTree) {
    // This functionality could be implemented using a visitor instead, however with this design, it
    // is easier to be certain that an error will always be issued if a tree kind is not recognized.
    // Only the most common tree kinds for synchronized expressions are supported.

    // Traverse the expression using 'tree', as 'lockExpressionTree' is used for error reporting.
    ExpressionTree tree = lockExpressionTree;

    while (true) {
      tree = TreeUtils.withoutParens(tree);

      switch (tree.getKind()) {
        case MEMBER_SELECT:
          if (!isTreeSymbolEffectivelyFinalOrUnmodifiable(tree)) {
            checker.reportError(tree, "lock.expression.not.final", lockExpressionTree);
            return;
          }
          tree = ((MemberSelectTree) tree).getExpression();
          break;
        case IDENTIFIER:
          if (!isTreeSymbolEffectivelyFinalOrUnmodifiable(tree)) {
            checker.reportError(tree, "lock.expression.not.final", lockExpressionTree);
          }
          return;
        case METHOD_INVOCATION:
          Element elem = TreeUtils.elementFromUse(tree);
          if (atypeFactory.getDeclAnnotationNoAliases(elem, Deterministic.class) == null
              && atypeFactory.getDeclAnnotationNoAliases(elem, Pure.class) == null) {
            checker.reportError(tree, "lock.expression.not.final", lockExpressionTree);
            return;
          }

          MethodInvocationTree methodInvocationTree = (MethodInvocationTree) tree;

          for (ExpressionTree argTree : methodInvocationTree.getArguments()) {
            ensureExpressionIsEffectivelyFinal(argTree);
          }

          tree = methodInvocationTree.getMethodSelect();
          break;
        default:
          checker.reportError(tree, "lock.expression.possibly.not.final", lockExpressionTree);
          return;
      }
    }
  }

  private void ensureExpressionIsEffectivelyFinal(
      final JavaExpression lockExpr,
      String expressionForErrorReporting,
      Tree treeForErrorReporting) {
    if (!atypeFactory.isExpressionEffectivelyFinal(lockExpr)) {
      checker.reportError(
          treeForErrorReporting, "lock.expression.not.final", expressionForErrorReporting);
    }
  }

  @Override
  public Void visitAnnotation(AnnotationTree tree, Void p) {
    ArrayList<AnnotationTree> annotationTreeList = new ArrayList<>(1);
    annotationTreeList.add(tree);
    List<AnnotationMirror> amList =
        TreeUtils.annotationsFromTypeAnnotationTrees(annotationTreeList);

    if (amList != null) {
      for (AnnotationMirror annotationMirror : amList) {
        if (atypeFactory.areSameByClass(annotationMirror, checkerGuardSatisfiedClass)) {
          issueErrorIfGuardSatisfiedAnnotationInUnsupportedLocation(tree);
        }
      }
    }

    return super.visitAnnotation(tree, p);
  }

  /**
   * Issues an error if a GuardSatisfied annotation is found in a location other than a method
   * return type or parameter (including the receiver).
   *
   * @param annotationTree AnnotationTree used for error reporting and to help determine that an
   *     array parameter has no GuardSatisfied annotations except on the array type
   */
  // TODO: Remove this method once @TargetLocations are enforced (i.e. once
  // issue https://github.com/typetools/checker-framework/issues/1919 is closed).
  private void issueErrorIfGuardSatisfiedAnnotationInUnsupportedLocation(
      AnnotationTree annotationTree) {
    TreePath currentPath = getCurrentPath();
    TreePath path = getPathForLocalVariableRetrieval(currentPath);
    if (path != null) {
      Tree tree = path.getLeaf();
      Tree.Kind kind = tree.getKind();

      if (kind == Tree.Kind.METHOD) {
        // The @GuardSatisfied annotation is on the return type.
        return;
      } else if (kind == Tree.Kind.VARIABLE) {
        VariableTree varTree = (VariableTree) tree;
        Tree varTypeTree = varTree.getType();
        if (varTypeTree != null) {
          TreePath parentPath = path.getParentPath();
          if (parentPath != null && parentPath.getLeaf().getKind() == Tree.Kind.METHOD) {
            Tree.Kind varTypeTreeKind = varTypeTree.getKind();
            if (varTypeTreeKind == Tree.Kind.ANNOTATED_TYPE) {
              AnnotatedTypeTree annotatedTypeTree = (AnnotatedTypeTree) varTypeTree;

              if (annotatedTypeTree.getUnderlyingType().getKind() != Tree.Kind.ARRAY_TYPE
                  || annotatedTypeTree.getAnnotations().contains(annotationTree)) {
                // Method parameter
                return;
              }
            } else if (varTypeTreeKind != Tree.Kind.ARRAY_TYPE) {
              // Method parameter or receiver
              return;
            }
          }
        }
      }
    }

    checker.reportError(annotationTree, "guardsatisfied.location.disallowed");
  }

  /**
   * The JavaExpression parser requires a path for retrieving the scope that will be used to resolve
   * local variables. One would expect that simply providing the path to an AnnotationTree would
   * work, since the compiler (as called by the org.checkerframework.javacutil.Resolver class) could
   * walk up the path from the AnnotationTree to determine the scope. Unfortunately this is not how
   * the compiler works. One must provide the path at the right level (not so deep that it results
   * in a symbol not being found, but not so high up that it is out of the scope at hand). This is a
   * problem when trying to retrieve local variables, since one could silently miss a local variable
   * in scope and accidentally retrieve a field with the same name. This method returns the correct
   * path for this purpose, given a path to an AnnotationTree.
   *
   * <p>Note: this is definitely necessary for local variable retrieval. It has not been tested
   * whether this is strictly necessary for fields or other identifiers.
   *
   * <p>Only call this method from visitAnnotation.
   *
   * @param path the TreePath whose leaf is an AnnotationTree
   * @return a TreePath that can be passed to methods in the Resolver class to locate local
   *     variables
   */
  private TreePath getPathForLocalVariableRetrieval(TreePath path) {
    assert path.getLeaf() instanceof AnnotationTree;

    // TODO: handle annotations in trees of kind NEW_CLASS (and add test coverage for this
    // scenario).
    // Currently an annotation in such a tree, such as "new @GuardedBy("foo") Object()",
    // results in a constructor.invocation error. This must be fixed first.

    path = path.getParentPath();

    if (path == null) {
      return null;
    }

    // A MODIFIERS tree for a VARIABLE or METHOD parent tree would be available at this level,
    // but it is not directly handled. Instead, its parent tree (one level higher) is handled.
    // Other tree kinds are also handled one level higher.

    path = path.getParentPath();

    if (path == null) {
      return null;
    }

    Tree tree = path.getLeaf();
    Tree.Kind kind = tree.getKind();

    switch (kind) {
      case ARRAY_TYPE:
      case VARIABLE:
      case TYPE_CAST:
      case INSTANCE_OF:
      case METHOD:
      case NEW_ARRAY:
      case TYPE_PARAMETER:
        // TODO: visitAnnotation does not currently visit annotations on wildcard bounds.
        // Address this for the Lock Checker somehow and enable these, as well as the
        // corresponding test cases in ChapterExamples.java
        // case EXTENDS_WILDCARD:
        // case SUPER_WILDCARD:
        return path;
      default:
        return null;
    }
  }

  /**
   * Returns true if the symbol for the given tree is final or effectively final. Package, class and
   * method symbols are unmodifiable and therefore considered final.
   */
  private boolean isTreeSymbolEffectivelyFinalOrUnmodifiable(Tree tree) {
    Element elem = TreeUtils.elementFromTree(tree);
    ElementKind ek = elem.getKind();
    return ek == ElementKind.PACKAGE
        || ek == ElementKind.CLASS
        || ek == ElementKind.METHOD
        || ElementUtils.isEffectivelyFinal(elem);
  }

  @Override
  @SuppressWarnings("interning:not.interned") // AST node comparison
  public Void visitIdentifier(IdentifierTree tree, Void p) {
    // If the identifier is a field accessed via an implicit this, then check the lock of this.
    // (All other field accesses are checked in visitMemberSelect.)
    if (TreeUtils.isFieldAccess(tree)) {
      Tree parent = getCurrentPath().getParentPath().getLeaf();
      // If the parent is not a member select, or if it is and the field is the expression,
      // then the field is accessed via an implicit this.
      if ((parent.getKind() != Kind.MEMBER_SELECT
              || ((MemberSelectTree) parent).getExpression() == tree)
          && !ElementUtils.isStatic(TreeUtils.elementFromUse(tree))) {
        AnnotationMirror guardedBy =
            atypeFactory.getSelfType(tree).getAnnotationInHierarchy(atypeFactory.GUARDEDBY);
        checkLockOfImplicitThis(tree, guardedBy);
      }
    }
    return super.visitIdentifier(tree, p);
  }

  @Override
  public Void visitBinary(BinaryTree binaryTree, Void p) {
    if (TreeUtils.isStringConcatenation(binaryTree)) {
      ExpressionTree leftTree = binaryTree.getLeftOperand();
      ExpressionTree rightTree = binaryTree.getRightOperand();

      boolean lhsIsString = TypesUtils.isString(TreeUtils.typeOf(leftTree));
      boolean rhsIsString = TypesUtils.isString(TreeUtils.typeOf(rightTree));
      if (!lhsIsString) {
        checkPreconditionsForImplicitToStringCall(leftTree);
      } else if (!rhsIsString) {
        checkPreconditionsForImplicitToStringCall(rightTree);
      }
    }

    return super.visitBinary(binaryTree, p);
  }

  @Override
  public Void visitCompoundAssignment(CompoundAssignmentTree node, Void p) {
    if (TreeUtils.isStringCompoundConcatenation(node)) {
      ExpressionTree rightTree = node.getExpression();
      if (!TypesUtils.isString(TreeUtils.typeOf(rightTree))) {
        checkPreconditionsForImplicitToStringCall(rightTree);
      }
    }

    return super.visitCompoundAssignment(node, p);
  }

  /**
   * Checks precondition for {@code tree} that is known to be the receiver of an implicit toString()
   * call. The receiver of toString() is defined in the annotated JDK to be @GuardSatisfied.
   * Therefore if the expression is guarded by a set of locks, the locks must be held prior to this
   * implicit call to toString().
   *
   * <p>Only call this method from visitBinary and visitCompoundAssignment.
   *
   * @param tree the Tree corresponding to the expression that is known to be the receiver of an
   *     implicit toString() call
   */
  // TODO: If and when the de-sugared .toString() tree is accessible from BaseTypeVisitor,
  // the toString() method call should be visited instead of doing this. This would result
  // in contracts.precondition errors being issued instead of
  // contracts.precondition.field, so it would be clear that
  // the error refers to an implicit method call, not a dereference (field access).
  private void checkPreconditionsForImplicitToStringCall(ExpressionTree tree) {
    AnnotationMirror gbAnno =
        atypeFactory
            .getAnnotatedType(tree)
            .getEffectiveAnnotationInHierarchy(atypeFactory.GUARDEDBY);
    checkLock(tree, gbAnno);
  }

  private void checkLockOfImplicitThis(Tree tree, AnnotationMirror gbAnno) {
    checkLockOfThisOrTree(tree, true, gbAnno);
  }

  private void checkLock(Tree tree, AnnotationMirror gbAnno) {
    checkLockOfThisOrTree(tree, false, gbAnno);
  }

  private void checkLockOfThisOrTree(Tree tree, boolean implicitThis, AnnotationMirror gbAnno) {
    if (gbAnno == null) {
      throw new BugInCF("LockVisitor.checkLock: gbAnno cannot be null");
    }
    if (atypeFactory.areSameByClass(gbAnno, GuardedByUnknown.class)
        || atypeFactory.areSameByClass(gbAnno, GuardedByBottom.class)) {
      checker.reportError(tree, "lock.not.held", "unknown lock " + gbAnno);
      return;
    } else if (atypeFactory.areSameByClass(gbAnno, GuardSatisfied.class)) {
      return;
    }

    List<LockExpression> expressions = getLockExpressions(implicitThis, gbAnno, tree);
    if (expressions.isEmpty()) {
      return;
    }

    LockStore store = atypeFactory.getStoreBefore(tree);
    for (LockExpression expression : expressions) {
      if (expression.error != null) {
        checker.reportError(tree, "expression.unparsable", expression.error.toString());
      } else if (expression.lockExpression == null) {
        checker.reportError(tree, "expression.unparsable", expression.expressionString);
      } else if (!isLockHeld(expression.lockExpression, store)) {
        checker.reportError(tree, "lock.not.held", expression.lockExpression.toString());
      }

      if (expression.error != null && expression.lockExpression != null) {
        ensureExpressionIsEffectivelyFinal(
            expression.lockExpression, expression.expressionString, tree);
      }
    }
  }

  private boolean isLockHeld(JavaExpression lockExpr, LockStore store) {
    if (store == null) {
      return false;
    }
    CFAbstractValue<?> value = store.getValue(lockExpr);
    if (value == null) {
      return false;
    }
    Set<AnnotationMirror> annos = value.getAnnotations();
    QualifierHierarchy hierarchy = atypeFactory.getQualifierHierarchy();
    AnnotationMirror lockAnno =
        hierarchy.findAnnotationInSameHierarchy(annos, atypeFactory.LOCKHELD);
    return lockAnno != null && atypeFactory.areSameByClass(lockAnno, LockHeld.class);
  }

  private List<LockExpression> getLockExpressions(
      boolean implicitThis, AnnotationMirror gbAnno, Tree tree) {

    List<String> expressions =
        AnnotationUtils.getElementValueArray(
            gbAnno, atypeFactory.guardedByValueElement, String.class, Collections.emptyList());

    if (expressions.isEmpty()) {
      return Collections.emptyList();
    }

    TreePath currentPath = getCurrentPath();

    TypeMirror enclosingType = TreeUtils.typeOf(TreePathUtil.enclosingClass(currentPath));
    JavaExpression pseudoReceiver = JavaExpression.getPseudoReceiver(currentPath, enclosingType);

    JavaExpression self;
    if (implicitThis) {
      self = pseudoReceiver;
    } else if (TreeUtils.isExpressionTree(tree)) {
      self = JavaExpression.fromTree((ExpressionTree) tree);
    } else {
      self = new Unknown(tree);
    }

    return CollectionsPlume.mapList(
        expression -> parseExpressionString(expression, currentPath, self), expressions);
  }

  /**
   * Parse a Java expression.
   *
   * @param expression the Java expression
   * @param path the path to the expression
   * @param itself the self expression
   * @return the parsed expression
   */
  private LockExpression parseExpressionString(
      String expression, TreePath path, JavaExpression itself) {

    LockExpression lockExpression = new LockExpression(expression);
    if (DependentTypesError.isExpressionError(expression)) {
      lockExpression.error = DependentTypesError.unparse(expression);
      return lockExpression;
    }

    Matcher selfReceiverMatcher = SELF_RECEIVER_PATTERN.matcher(expression);
    try {
      if (selfReceiverMatcher.matches()) {
        String remainingExpression = selfReceiverMatcher.group(2);
        if (remainingExpression == null || remainingExpression.isEmpty()) {
          lockExpression.lockExpression = itself;
          if (!atypeFactory.isExpressionEffectivelyFinal(lockExpression.lockExpression)) {
            checker.reportError(
                path.getLeaf(), "lock.expression.not.final", lockExpression.lockExpression);
          }
          return lockExpression;
        } else {
          lockExpression.lockExpression =
              StringToJavaExpression.atPath(
                  itself.toString() + "." + remainingExpression, path, checker);
          if (!atypeFactory.isExpressionEffectivelyFinal(lockExpression.lockExpression)) {
            checker.reportError(
                path.getLeaf(), "lock.expression.not.final", lockExpression.lockExpression);
          }
          return lockExpression;
        }
      } else {
        lockExpression.lockExpression = StringToJavaExpression.atPath(expression, path, checker);
        return lockExpression;
      }
    } catch (JavaExpressionParseException ex) {
      lockExpression.error = new DependentTypesError(expression, ex);
      return lockExpression;
    }
  }

  private static class LockExpression {
    final String expressionString;
    JavaExpression lockExpression = null;
    DependentTypesError error = null;

    LockExpression(String expression) {
      this.expressionString = expression;
    }
  }
}
