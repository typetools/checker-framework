package org.checkerframework.dataflow.util;

import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.CatchTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ThrowTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.Deterministic;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.dataflow.qual.SideEffectsOnly;
import org.checkerframework.javacutil.AnnotationProvider;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreePathUtil;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;

/**
 * A visitor that determines the purity (as defined by {@link
 * org.checkerframework.dataflow.qual.SideEffectFree}, {@link
 * org.checkerframework.dataflow.qual.Deterministic}, and {@link
 * org.checkerframework.dataflow.qual.Pure}) of a statement or expression. The entry point is method
 * {@link #checkPurity}.
 *
 * <p>This class does not check {@link org.checkerframework.dataflow.qual.SideEffectsOnly}, which is
 * the other purity annotation. Verifying {@code @SideEffectsOnly} requires parsing the Java
 * expressions in the annotation and viewpoint-adapting them at each call site, which this module
 * cannot do. {@code org.checkerframework.common.basetype.DisallowedSideEffects}, which {@code
 * BaseTypeVisitor} calls, checks {@code @SideEffectsOnly}.
 *
 * @see SideEffectFree
 * @see Deterministic
 * @see Pure
 */
public final class PurityChecker {

  /** Do not instantiate. */
  private PurityChecker() {
    throw new Error("Do not instantiate");
  }

  /**
   * Compute whether the given statement is side-effect-free, deterministic, or both. Returns a
   * result that can be queried.
   *
   * @param statement the statement to check
   * @param annoProvider the annotation provider
   * @param enclosingMethod the method declaration that lexically encloses {@code statement}, or
   *     null if none does. A call to the functional method of one of that method's
   *     functional-interface parameters has the method's own purity; see {@link
   *     #isFunctionalInterfaceParameter}. Pass the enclosing method for a lambda body too: the body
   *     is checked against the functional method that the lambda implements, but the enclosing
   *     method's parameters still hold values that its caller was required to check, whenever the
   *     lambda runs. Pass null for an arbitrary expression, which no method's contract governs.
   * @param env the processing environment; used only if {@code enclosingMethod} is non-null
   * @param assumeSideEffectFree true if all methods should be assumed to be @SideEffectFree
   * @param assumeDeterministic true if all methods should be assumed to be @Deterministic
   * @param assumePureGetters true if all getter methods should be assumed to be @Pure
   * @return information about whether the given statement is side-effect-free, deterministic, or
   *     both
   */
  public static PurityResult checkPurity(
      TreePath statement,
      AnnotationProvider annoProvider,
      @Nullable MethodTree enclosingMethod,
      @Nullable ProcessingEnvironment env,
      boolean assumeSideEffectFree,
      boolean assumeDeterministic,
      boolean assumePureGetters) {
    PurityCheckerHelper helper =
        new PurityCheckerHelper(
            annoProvider,
            enclosingMethod,
            env,
            assumeSideEffectFree,
            assumeDeterministic,
            assumePureGetters);
    helper.scan(statement, null);
    return helper.purityResult;
  }

  /**
   * Returns the kinds of purity that a method promises for the functional method of its
   * functional-interface parameters. At a call to {@code method}, each argument passed to a
   * functional-interface parameter is required to have those kinds, so within the body the
   * parameters may be assumed to have them.
   *
   * @param annoProvider the annotation provider
   * @param method a method or constructor, or null
   * @return the purity kinds that {@code method} requires of its functional-interface arguments;
   *     empty if {@code method} is null or has no purity annotation
   */
  public static EnumSet<PurityKind> functionalParameterKinds(
      AnnotationProvider annoProvider, @Nullable ExecutableElement method) {
    if (method == null) {
      return EnumSet.noneOf(PurityKind.class);
    }
    EnumSet<PurityKind> result = EnumSet.copyOf(PurityUtils.getPurityKinds(annoProvider, method));
    if (annoProvider.getDeclAnnotation(method, SideEffectsOnly.class) != null) {
      // A @SideEffectsOnly method may modify the listed expressions, but the code it is handed
      // may not modify anything.
      result.add(PurityKind.SIDE_EFFECT_FREE);
    }
    return result;
  }

  /**
   * Returns true if {@code expr} is an effectively final formal parameter of {@code method} whose
   * type is a functional interface.
   *
   * <p>The expression must be the parameter itself. A local variable that aliases it, a field that
   * holds it, or the result of a call does not qualify: only the parameter is known to hold the
   * value that the caller supplied. The parameter must be effectively final because a body that
   * reassigns it no longer holds that value.
   *
   * @param expr an expression, or null
   * @param method a method or constructor declaration, or null
   * @param env the processing environment
   * @return true if {@code expr} is an effectively final functional-interface parameter of {@code
   *     method}
   */
  public static boolean isFunctionalInterfaceParameter(
      @Nullable ExpressionTree expr, @Nullable MethodTree method, ProcessingEnvironment env) {
    return functionalInterfaceParameterType(expr, method, env) != null;
  }

  /**
   * Returns true if {@code expr} is an effectively final functional-interface parameter of {@code
   * method} and {@code invoked} is the functional method of {@code expr}'s type.
   *
   * <p>The code that such an expression denotes has the purity that {@code method} promises,
   * because at every call to {@code method} the argument was required to have it. Only the
   * functional method carries that guarantee; a default method such as {@code Function.andThen}
   * does not.
   *
   * @param expr an expression, or null
   * @param invoked a method that {@code expr} is used to invoke or to refer to
   * @param method a method or constructor declaration, or null
   * @param env the processing environment
   * @return true if {@code invoked} is the functional method of a functional-interface parameter of
   *     {@code method}
   */
  public static boolean isFunctionalMethodOfParameter(
      @Nullable ExpressionTree expr,
      ExecutableElement invoked,
      @Nullable MethodTree method,
      ProcessingEnvironment env) {
    TypeMirror parameterType = functionalInterfaceParameterType(expr, method, env);
    if (parameterType == null) {
      return false;
    }
    ExecutableElement functionalMethod = TypesUtils.findFunction(parameterType, env);
    @SuppressWarnings("interning:not.interned") // Checking for exact object.
    boolean isFunctionalMethod = invoked == functionalMethod;
    return isFunctionalMethod
        || env.getElementUtils()
            .overrides(invoked, functionalMethod, (TypeElement) invoked.getEnclosingElement());
  }

  /**
   * Returns the functional interface type of {@code expr}, if {@code expr} is an effectively final
   * formal parameter of {@code method} whose type is a functional interface; otherwise returns
   * null.
   *
   * @param expr an expression, or null
   * @param method a method or constructor declaration, or null
   * @param env the processing environment
   * @return the functional interface type of {@code expr}, or null
   */
  private static @Nullable TypeMirror functionalInterfaceParameterType(
      @Nullable ExpressionTree expr, @Nullable MethodTree method, ProcessingEnvironment env) {
    if (expr == null
        || method == null
        || !(TreeUtils.withoutParens(expr) instanceof IdentifierTree id)) {
      return null;
    }
    Element element = TreeUtils.elementFromTree(id);
    if (element == null
        || element.getKind() != ElementKind.PARAMETER
        || !ElementUtils.isEffectivelyFinal(element)) {
      return null;
    }
    // Test membership in the parameter list rather than the enclosing element, because javac
    // gives a lambda's parameters the enclosing method as their enclosing element.  A lambda's
    // parameter is not checked at any call site.
    if (!isParameterOf(element, method)) {
      return null;
    }
    return functionalInterfaceType(element.asType(), env);
  }

  /**
   * Returns true if {@code element} is one of {@code method}'s formal parameters.
   *
   * @param element an element
   * @param method a method or constructor declaration
   * @return true if {@code element} is a formal parameter of {@code method}
   */
  @SuppressWarnings("interning:not.interned") // Checking for exact object.
  private static boolean isParameterOf(Element element, MethodTree method) {
    for (VariableTree parameter : method.getParameters()) {
      if (TreeUtils.elementFromDeclaration(parameter) == element) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns {@code type}, or its upper bound if it is a type variable, if that is a functional
   * interface type; otherwise returns null. Use {@link TypesUtils#findFunction} on the result to
   * obtain the functional method.
   *
   * @param type a type
   * @param env the processing environment
   * @return the functional interface type that {@code type} denotes, or null
   */
  public static @Nullable TypeMirror functionalInterfaceType(
      TypeMirror type, ProcessingEnvironment env) {
    if (type.getKind() == TypeKind.TYPEVAR) {
      type = TypesUtils.upperBound(type);
    }
    if (type.getKind() != TypeKind.DECLARED && type.getKind() != TypeKind.INTERSECTION) {
      return null;
    }
    return TypesUtils.isFunctionalInterface(type, env) ? type : null;
  }

  /**
   * Result of the {@link PurityChecker}. Can be queried regarding whether a given tree was
   * side-effect-free, deterministic, or both; also gives reasons if the answer is "no".
   */
  public static class PurityResult {

    /** Creates a new PurityResult. */
    public PurityResult() {}

    /**
     * A reason that a method is impure: a tree, and a message key explaining what is wrong with it.
     *
     * @param tree the tree that makes the method impure
     * @param msgId the message key for the reason that {@code tree} makes the method impure
     */
    public record ImpurityReason(Tree tree, String msgId) {}

    /** Reasons that the referenced method is not side-effect-free. */
    protected final List<ImpurityReason> notSEFreeReasons = new ArrayList<>(1);

    /** Reasons that the referenced method is not deterministic. */
    protected final List<ImpurityReason> notDetReasons = new ArrayList<>(1);

    /** Reasons that the referenced method is not side-effect-free and deterministic. */
    protected final List<ImpurityReason> notBothReasons = new ArrayList<>(1);

    /**
     * Contains the varieties of purity that the expression has. Starts out with the purities that a
     * method body can be analyzed for ({@link PurityKind#SIDE_EFFECT_FREE} and {@link
     * PurityKind#DETERMINISTIC}), and elements are removed from it as violations are found.
     */
    protected EnumSet<PurityKind> kinds =
        EnumSet.of(PurityKind.SIDE_EFFECT_FREE, PurityKind.DETERMINISTIC);

    /**
     * Returns the kinds of purity that the method has.
     *
     * @return the kinds of purity that the method has
     */
    public EnumSet<PurityKind> getKinds() {
      return kinds;
    }

    /**
     * Is the method pure w.r.t. a given set of kinds?
     *
     * @param otherKinds the varieties of purity to check
     * @return true if the method is pure with respect to all the given kinds
     */
    public boolean isPure(EnumSet<PurityKind> otherKinds) {
      return kinds.containsAll(otherKinds);
    }

    /**
     * Returns the reasons why the method is not side-effect-free.
     *
     * @return the reasons why the method is not side-effect-free
     */
    public List<ImpurityReason> getNotSEFreeReasons() {
      return notSEFreeReasons;
    }

    /**
     * Add a reason why the method is not side-effect-free.
     *
     * @param t a tree
     * @param msgId why the tree is not side-effect-free
     */
    public void addNotSEFreeReason(Tree t, String msgId) {
      notSEFreeReasons.add(new ImpurityReason(t, msgId));
      kinds.remove(PurityKind.SIDE_EFFECT_FREE);
    }

    /**
     * Returns the reasons why the method is not deterministic.
     *
     * @return the reasons why the method is not deterministic
     */
    public List<ImpurityReason> getNotDetReasons() {
      return notDetReasons;
    }

    /**
     * Add a reason why the method is not deterministic.
     *
     * @param t a tree
     * @param msgId why the tree is not deterministic
     */
    public void addNotDetReason(Tree t, String msgId) {
      notDetReasons.add(new ImpurityReason(t, msgId));
      kinds.remove(PurityKind.DETERMINISTIC);
    }

    /**
     * Returns the reasons why the method is not both side-effect-free and deterministic.
     *
     * @return the reasons why the method is not both side-effect-free and deterministic
     */
    public List<ImpurityReason> getNotBothReasons() {
      return notBothReasons;
    }

    /**
     * Add a reason why the method is not both side-effect-free and deterministic.
     *
     * @param t tree
     * @param msgId why the tree is not deterministic and side-effect-free
     */
    public void addNotBothReason(Tree t, String msgId) {
      notBothReasons.add(new ImpurityReason(t, msgId));
      kinds.remove(PurityKind.DETERMINISTIC);
      kinds.remove(PurityKind.SIDE_EFFECT_FREE);
    }

    @Override
    public String toString() {
      return String.join(
          System.lineSeparator(),
          "PurityResult{",
          "  notSEF: " + notSEFreeReasons,
          "  notDet: " + notDetReasons,
          "  notBoth: " + notBothReasons,
          "}");
    }
  }

  // TODO: It would be possible to improve efficiency by visiting fewer nodes.  This would require
  // overriding more visit* methods.  I'm not sure whether such an optimization would be worth it.

  /**
   * Helper class to keep {@link PurityChecker}'s interface clean.
   *
   * <p>The scanner is run on a single statement, not on a class or method.
   */
  protected static class PurityCheckerHelper extends TreePathScanner<Void, Void> {

    /** The purity result. */
    PurityResult purityResult = new PurityResult();

    /** The annotation provider (typically an AnnotatedTypeFactory). */
    protected final AnnotationProvider annoProvider;

    /** The method declaration that lexically encloses the checked statement, or null if none. */
    private final @Nullable MethodTree enclosingMethod;

    /** The processing environment; null if {@link #enclosingMethod} is null. */
    private final @Nullable ProcessingEnvironment env;

    /**
     * The purity that {@link #enclosingMethod} promises for the functional method of its
     * functional-interface parameters. Empty if there is no enclosing method or it has no purity
     * annotation, in which case no call gets the assumption.
     */
    private final EnumSet<PurityKind> functionalParameterKinds;

    /**
     * True if all methods should be assumed to be @SideEffectFree, for the purposes of
     * org.checkerframework.dataflow analysis.
     */
    private final boolean assumeSideEffectFree;

    /**
     * True if all methods should be assumed to be @Deterministic, for the purposes of
     * org.checkerframework.dataflow analysis.
     */
    private final boolean assumeDeterministic;

    /**
     * True if all getter methods should be assumed to be @SideEffectFree and @Deterministic, for
     * the purposes of org.checkerframework.dataflow analysis.
     */
    private final boolean assumePureGetters;

    /**
     * Create a PurityCheckerHelper.
     *
     * @param annoProvider the annotation provider
     * @param enclosingMethod the method declaration that lexically encloses the checked statement,
     *     or null if none does
     * @param env the processing environment; used only if {@code enclosingMethod} is non-null
     * @param assumeSideEffectFree true if all methods should be assumed to be @SideEffectFree
     * @param assumeDeterministic true if all methods should be assumed to be @Deterministic
     * @param assumePureGetters true if getter methods should be assumed to be @Pure
     */
    public PurityCheckerHelper(
        AnnotationProvider annoProvider,
        @Nullable MethodTree enclosingMethod,
        @Nullable ProcessingEnvironment env,
        boolean assumeSideEffectFree,
        boolean assumeDeterministic,
        boolean assumePureGetters) {
      this.annoProvider = annoProvider;
      this.enclosingMethod = enclosingMethod;
      this.env = env;
      this.functionalParameterKinds =
          enclosingMethod == null
              ? EnumSet.noneOf(PurityKind.class)
              : functionalParameterKinds(
                  annoProvider, TreeUtils.elementFromDeclaration(enclosingMethod));
      this.assumeSideEffectFree = assumeSideEffectFree;
      this.assumeDeterministic = assumeDeterministic;
      this.assumePureGetters = assumePureGetters;
    }

    /**
     * Returns true if {@code tree} invokes the functional method of an effectively final
     * functional-interface parameter of the method being checked.
     *
     * <p>Such a call has the purity that the method promises, because at every call to the method
     * the argument was required to have it.
     *
     * @param tree a method invocation
     * @param invoked the invoked method
     * @return true if {@code tree} calls a functional-interface parameter of the enclosing method
     */
    private boolean isCallOnFunctionalInterfaceParameter(
        MethodInvocationTree tree, ExecutableElement invoked) {
      if (functionalParameterKinds.isEmpty()) {
        // There is no enclosing method, or it promises nothing.
        return false;
      }
      ProcessingEnvironment env = this.env;
      if (env == null) {
        return false;
      }
      return isFunctionalMethodOfParameter(
          TreeUtils.getReceiverTree(tree), invoked, enclosingMethod, env);
    }

    @Override
    public Void visitCatch(CatchTree tree, Void ignore) {
      purityResult.addNotDetReason(tree, "catch");
      return super.visitCatch(tree, ignore);
    }

    /**
     * Evaluating a lambda expression creates an object; it does not run the lambda's body.
     *
     * <p>Creating an object is not deterministic, just as a {@code new} expression is not; see
     * {@link #visitNewClass}.
     *
     * <p>The body's effects occur where the lambda's functional method is invoked, and that
     * invocation is checked like any other method call. Therefore, do not scan the body. The body
     * is checked elsewhere, against the purity annotations on the functional method that the lambda
     * implements; see {@code BaseTypeVisitor#checkLambdaPurity}.
     *
     * @param tree a lambda expression
     * @param ignore an unused parameter
     * @return null
     */
    @Override
    public Void visitLambdaExpression(LambdaExpressionTree tree, Void ignore) {
      purityResult.addNotDetReason(tree, "object.creation");
      return null;
    }

    /**
     * Evaluating a method reference creates an object; it does not run the referenced method.
     *
     * <p>Creating an object is not deterministic, just as a {@code new} expression is not; see
     * {@link #visitNewClass}. JLS 15.13.3 leaves it unspecified whether two evaluations of the same
     * method reference produce the same object.
     *
     * <p>The referenced method's effects occur where the functional method is invoked, and that
     * invocation is checked like any other method call.
     *
     * <p>Do scan the children. The qualifier expression of {@code EXPR::m} is evaluated where the
     * method reference appears, unlike the body of a lambda.
     *
     * @param tree a method reference
     * @param ignore an unused parameter
     * @return null
     */
    @Override
    public Void visitMemberReference(MemberReferenceTree tree, Void ignore) {
      purityResult.addNotDetReason(tree, "object.creation");
      return super.visitMemberReference(tree, ignore);
    }

    /**
     * Declaring a local or anonymous class has no side effect. The class's methods are checked
     * against their own purity annotations, like the methods of any other class.
     *
     * <p>Do scan the class's non-method members. Attributing a field initializer or an initializer
     * block to the method that contains the class declaration is conservative: the effect is
     * reported even if the class is never instantiated.
     *
     * @param tree a class declaration
     * @param ignore an unused parameter
     * @return null
     */
    @Override
    public Void visitClass(ClassTree tree, Void ignore) {
      for (Tree member : tree.getMembers()) {
        if (!(member instanceof MethodTree)) {
          scan(member, ignore);
        }
      }
      return null;
    }

    @Override
    public Void visitMethodInvocation(MethodInvocationTree tree, Void ignore) {
      ExecutableElement elt = TreeUtils.elementFromUse(tree);
      EnumSet<PurityKind> eltPurityKinds = PurityUtils.getPurityKinds(annoProvider, elt);
      if (isCallOnFunctionalInterfaceParameter(tree, elt)) {
        eltPurityKinds = EnumSet.copyOf(eltPurityKinds);
        eltPurityKinds.addAll(functionalParameterKinds);
      }

      boolean pureGetter = assumePureGetters && ElementUtils.isGetter(elt);
      boolean seFree =
          assumeSideEffectFree
              || pureGetter
              || eltPurityKinds.contains(PurityKind.SIDE_EFFECT_FREE);
      boolean det =
          assumeDeterministic
              || pureGetter
              || eltPurityKinds.contains(PurityKind.DETERMINISTIC)
              // A side-effect-free method with no return value is deterministic:  two calls
              // return the same (absent) value.  This includes a `this()` or `super()` call,
              // whose element's return type is void.
              || (seFree && elt.getReturnType().getKind() == TypeKind.VOID);
      if (!det && !seFree) {
        purityResult.addNotBothReason(tree, "call");
      } else if (!det) {
        purityResult.addNotDetReason(tree, "call");
      } else if (!seFree) {
        purityResult.addNotSEFreeReason(tree, "call");
      }
      return super.visitMethodInvocation(tree, ignore);
    }

    @Override
    public Void visitNewClass(NewClassTree tree, Void ignore) {
      // Ordinarily, "new MyClass()" is forbidden.  It is permitted, however, when it is the
      // expression in "throw EXPR;".  (In the future, more expressions could be permitted.)
      //
      // The expression in "throw EXPR;" is allowed to be non-@Deterministic, so long as it is
      // not within a catch block that could catch an exception that the statement throws.
      // For example, EXPR can be object creation (a "new" expression) or can call a
      // non-deterministic method.
      //
      // Coarse rule (currently implemented):
      //  * permit only "throw new SomeExpression(args)", where the constructor is
      //    @SideEffectFree and the args are pure, and forbid all enclosing try statements
      //    that have a catch clause.
      // More precise rule:
      //  * permit other non-deterministic expressions within throw (at which time move this
      //    logic to visitThrow()).
      //  * the only bad try statements are those with a catch block that is:
      //     * unchecked exceptions
      //        * checked = Exception or lower, but excluding RuntimeException and its
      //          subclasses
      //     * super- or sub-classes of the type of _expr_
      //        * if _expr_ is exactly "new SomeException", this can be changed to just
      //          "superclasses of SomeException".
      //     * super- or sub-classes of exceptions declared to be thrown by any component of
      //       _expr_.
      //     * need to check every containing try statement, not just the nearest enclosing
      //       one.

      // Object creation is usually prohibited, but permit "throw new SomeException();" if it
      // is not contained within any try statement that has a catch clause.  (There is no need
      // to check the latter condition, because the Purity Checker forbids all catch
      // statements.)
      Tree parent = getCurrentPath().getParentPath().getLeaf();
      boolean okThrowDeterministic = parent instanceof ThrowTree;

      ExecutableElement ctorElement = TreeUtils.elementFromUse(tree);
      boolean deterministic =
          assumeDeterministic
              || okThrowDeterministic
              // No need to check assumePureGetters because a constructor is never a
              // getter.
              || PurityUtils.isDeterministic(annoProvider, ctorElement);
      boolean sideEffectFree =
          assumeSideEffectFree || PurityUtils.isSideEffectFree(annoProvider, ctorElement);
      // This does not use "addNotBothReason" because the reasons are different:  one is
      // because the constructor is called at all, and the other is because the constructor is
      // not side-effect-free.
      if (!deterministic) {
        purityResult.addNotDetReason(tree, "object.creation");
      }
      if (!sideEffectFree) {
        purityResult.addNotSEFreeReason(tree, "call");
      }

      // TODO: if okThrowDeterministic, permit arguments to the newClass to be
      // non-deterministic (don't add those to purityResult), but still don't permit them to
      // have side effects.  This should probably wait until a rewrite of the Purity Checker.
      return super.visitNewClass(tree, ignore);
    }

    @Override
    public Void visitAssignment(AssignmentTree tree, Void ignore) {
      ExpressionTree variable = tree.getVariable();
      assignmentCheck(variable);
      return super.visitAssignment(tree, ignore);
    }

    @Override
    public Void visitUnary(UnaryTree tree, Void ignore) {
      switch (tree.getKind()) {
        case POSTFIX_DECREMENT, POSTFIX_INCREMENT, PREFIX_DECREMENT, PREFIX_INCREMENT -> {
          ExpressionTree expression = tree.getExpression();
          assignmentCheck(expression);
        }
        default -> {
          // Nothing to do
        }
      }
      return super.visitUnary(tree, ignore);
    }

    /**
     * Returns true if {@code variable} is permitted on the left-hand-side of an assignment.
     *
     * @param variable the lhs to check
     */
    protected void assignmentCheck(ExpressionTree variable) {
      variable = TreeUtils.withoutParens(variable);
      VariableElement fieldElt = TreeUtils.asFieldAccess(variable);
      if (fieldElt != null && isFieldInCurrentClass(fieldElt) && inConstructorNotInLambda()) {
        // assigning a field in a constructor
        // TODO: add a check for ArrayAccessTree too.
        return;
      }
      if (TreeUtils.isFieldAccess(variable)) {
        // lhs is a field access
        purityResult.addNotBothReason(variable, "assign.field");
      } else if (variable instanceof ArrayAccessTree) {
        // lhs is array access
        purityResult.addNotBothReason(variable, "assign.array");
      } else {
        // lhs is a local variable
        assert isLocalVariable(variable);
      }
    }

    /**
     * Returns true if the current path is within a constructor, a field initializer, or an
     * initializer block of the innermost enclosing class, and is not within a lambda expression.
     *
     * <p>{@link #assignmentCheck} permits a constructor to assign to a field of its own class,
     * because the object is not yet visible to other code. That reasoning does not extend to a
     * lambda that a constructor creates: the lambda's body may run long after the constructor has
     * returned, when the object is visible.
     *
     * @return true if the current path is within a constructor, field initializer, or initializer
     *     block, and within no lambda expression
     */
    private boolean inConstructorNotInLambda() {
      // The search stops at the innermost enclosing class, because a method or lambda outside
      // that class does not contain the current path's code:  the code of a field initializer
      // or initializer block runs when the class is instantiated or initialized.
      for (TreePath path = getCurrentPath(); path != null; path = path.getParentPath()) {
        Tree leaf = path.getLeaf();
        if (leaf instanceof MethodTree methodTree) {
          return TreeUtils.isConstructor(methodTree);
        } else if (leaf instanceof LambdaExpressionTree) {
          return false;
        } else if (TreeUtils.classTreeKinds().contains(leaf.getKind())) {
          // This is a field initializer or an initializer block.
          return true;
        }
      }
      // This is a field initializer or an initializer block; the scan started within it, so no
      // class declaration was encountered.
      return true;
    }

    /**
     * Returns true if the given field is defined by the current class.
     *
     * @param fieldElt a field
     * @return true if the given field is defined by the current class
     */
    private boolean isFieldInCurrentClass(VariableElement fieldElt) {
      ClassTree currentTypeTree = TreePathUtil.enclosingClass(getCurrentPath());
      assert currentTypeTree != null : "@AssumeAssertion(nullness)";
      TypeElement currentType = TreeUtils.elementFromDeclaration(currentTypeTree);
      assert currentType != null : "@AssumeAssertion(nullness)";
      TypeElement definesField = ElementUtils.enclosingTypeElement(fieldElt);
      assert definesField != null : "@AssumeAssertion(nullness)";
      return currentType.equals(definesField);
    }

    /**
     * Checks if the argument is a local variable.
     *
     * @param variable the tree to check
     * @return true if the argument is a local variable
     */
    protected boolean isLocalVariable(ExpressionTree variable) {
      return variable instanceof IdentifierTree && !TreeUtils.isFieldAccess(variable);
    }

    @Override
    public Void visitCompoundAssignment(CompoundAssignmentTree tree, Void ignore) {
      ExpressionTree variable = tree.getVariable();
      assignmentCheck(variable);
      return super.visitCompoundAssignment(tree, ignore);
    }
  }
}
