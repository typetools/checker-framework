package org.checkerframework.common.basetype;

import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.dataflow.expression.JavaExpressionParseException;
import org.checkerframework.dataflow.expression.ThisReference;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.dataflow.qual.SideEffectsOnly;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.util.StringToJavaExpression;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TreeUtils;
import org.plumelib.util.IPair;
import org.plumelib.util.UnionFind;

/**
 * Scanner that collects the expressions a method side-effects, beyond those listed in its {@link
 * SideEffectsOnly} annotation, and reports an error for each one.
 *
 * <p>Clients use the static method {@link #checkSideEffectsOnly}.
 */
public class DisallowedSideEffects extends TreePathScanner<Void, Void> {

  /** Expressions the method side-effects that are not in its {@link SideEffectsOnly} annotation. */
  protected final List<IPair<Tree, JavaExpression>> disallowedSideEffects = new ArrayList<>(1);

  /**
   * List of expressions specified as annotation arguments in the {@link SideEffectsOnly} annotation
   * of the method being checked.
   */
  protected final List<JavaExpression> sideEffectsOnlyExpressionsFromAnnotation;

  /**
   * Groups expressions into sets, where all the elements in each set might be aliased to one other.
   */
  protected final UnionFind<JavaExpression> aliasedExpressions =
      new UnionFind<>(null, JavaExpression::containsAsReceiver);

  /** The checker to use. */
  protected final BaseTypeChecker checker;

  /** The {@code SideEffectsOnly.value} argument/element. */
  protected final ExecutableElement sideEffectsOnlyValueElement;

  /**
   * Creates a new DisallowedSideEffects.
   *
   * @param sideEffectsOnlyExpressions the arguments/values of the {@link SideEffectsOnly}
   *     annotation of the method being checked
   * @param checker the checker to use
   * @param sideEffectsOnlyValueElement the {@code SideEffectsOnly.value} argument/element
   */
  protected DisallowedSideEffects(
      List<JavaExpression> sideEffectsOnlyExpressions,
      BaseTypeChecker checker,
      ExecutableElement sideEffectsOnlyValueElement) {
    this.sideEffectsOnlyExpressionsFromAnnotation = sideEffectsOnlyExpressions;
    this.checker = checker;
    this.sideEffectsOnlyValueElement = sideEffectsOnlyValueElement;
  }

  /**
   * Issues warnings about side effects beyond the {@code @SideEffectsOnly} annotation.
   *
   * @param statement the method body to check
   * @param sideEffectsOnlyExpressions the values in the {@link SideEffectsOnly} annotation
   * @param checker the checker to use
   * @param methodTree the method, used for diagnostics
   * @param sideEffectsOnlyValueElement the {@code SideEffectsOnly.value} argument/element
   */
  public static void checkSideEffectsOnly(
      TreePath statement,
      List<JavaExpression> sideEffectsOnlyExpressions,
      BaseTypeChecker checker,
      MethodTree methodTree,
      ExecutableElement sideEffectsOnlyValueElement) {
    DisallowedSideEffects scanner =
        new DisallowedSideEffects(sideEffectsOnlyExpressions, checker, sideEffectsOnlyValueElement);
    scanner.scan(statement, null);

    for (IPair<Tree, JavaExpression> s : scanner.disallowedSideEffects) {
      checker.reportError(
          s.first, "purity.incorrect.sideeffectsonly", methodTree.getName(), s.second.toString());
    }
  }

  // A `this(...)` or `super(...)` call is a MethodInvocationTree, so `visitMethodInvocation`
  // handles it.  A `new` expression is handled by `visitNewClass`.

  @Override
  public Void visitMethodInvocation(MethodInvocationTree node, Void aVoid) {
    Element invokedElem = TreeUtils.elementFromUse(node);
    if (invokedElem == null || TreeUtils.isEnumSuperCall(node)) {
      return super.visitMethodInvocation(node, aVoid);
    }
    AnnotatedTypeFactory atypeFactory = checker.getTypeFactory();
    boolean isMarkedPure = atypeFactory.getDeclAnnotation(invokedElem, Pure.class) != null;
    boolean isMarkedSideEffectFree =
        atypeFactory.getDeclAnnotation(invokedElem, SideEffectFree.class) != null;
    if (isMarkedPure || isMarkedSideEffectFree) {
      // The callee modifies nothing.
      // TODO: Should all the checking be integrated together?
      return super.visitMethodInvocation(node, aVoid);
    }

    AnnotationMirror seOnlyAnnotation =
        atypeFactory.getDeclAnnotation(invokedElem, SideEffectsOnly.class);
    if (seOnlyAnnotation == null) {
      // The callee has no side-effect annotation, so it might modify arbitrary state, including
      // static state and other state not reachable from its receiver and arguments.
      // A different message key than `purity.incorrect.sideeffectsonly` is used because the
      // subject of this message is the callee, not the method being checked.
      checker.reportError(node, "purity.unknown.sideeffectsonly", invokedElem.getSimpleName());
      return super.visitMethodInvocation(node, aVoid);
    }

    // The callee modifies at most the expressions listed in its own `@SideEffectsOnly` annotation.
    for (JavaExpression expr : calleeSideEffectedExpressions(node, invokedElem, seOnlyAnnotation)) {
      if (isDisallowedSideEffectedExpression(expr)) {
        disallowedSideEffects.add(IPair.of(node, expr));
      }
    }
    return super.visitMethodInvocation(node, aVoid);
  }

  /**
   * Returns the expressions that the invoked method side-effects: the arguments/elements of its
   * {@link SideEffectsOnly} annotation, view-adapted to the given call site.
   *
   * <p>If an expression cannot be parsed at the call site, this reports {@code
   * purity.unknown.sideeffectsonly} and returns an empty list. That is fail-closed: the checker
   * cannot tell what the callee modifies, so it says so rather than silently treating the callee as
   * modifying less than it was declared to.
   *
   * @param node a call to a method that is annotated with {@link SideEffectsOnly}
   * @param invokedElem the invoked method
   * @param seOnlyAnnotation the invoked method's {@link SideEffectsOnly} annotation
   * @return the expressions that the invoked method side-effects, view-adapted to {@code node}
   */
  protected List<JavaExpression> calleeSideEffectedExpressions(
      MethodInvocationTree node, Element invokedElem, AnnotationMirror seOnlyAnnotation) {
    List<String> exprStrings =
        AnnotationUtils.getElementValueArray(
            seOnlyAnnotation, sideEffectsOnlyValueElement, String.class);
    List<JavaExpression> result = new ArrayList<>(exprStrings.size());
    for (String exprString : exprStrings) {
      try {
        result.add(StringToJavaExpression.atMethodInvocation(exprString, node, checker));
      } catch (JavaExpressionParseException ex) {
        // The parse error itself is reported at the callee's declaration, by
        // BaseTypeVisitor.checkPurityAnnotations.
        checker.reportError(node, "purity.unknown.sideeffectsonly", invokedElem.getSimpleName());
        return Collections.emptyList();
      }
    }
    return result;
  }

  @Override
  public Void visitNewClass(NewClassTree node, Void aVoid) {
    ExecutableElement constructorElt = TreeUtils.elementFromUse(node);
    if (constructorElt == null) {
      return super.visitNewClass(node, aVoid);
    }
    AnnotatedTypeFactory atypeFactory = checker.getTypeFactory();
    boolean isMarkedPure = atypeFactory.getDeclAnnotation(constructorElt, Pure.class) != null;
    boolean isMarkedSideEffectFree =
        atypeFactory.getDeclAnnotation(constructorElt, SideEffectFree.class) != null;
    if (isMarkedPure || isMarkedSideEffectFree) {
      // The constructor modifies nothing that existed before it was called.
      return super.visitNewClass(node, aVoid);
    }

    AnnotationMirror seOnlyAnnotation =
        atypeFactory.getDeclAnnotation(constructorElt, SideEffectsOnly.class);
    if (seOnlyAnnotation == null) {
      // The constructor has no side-effect annotation, so it might modify arbitrary state.
      checker.reportError(node, "purity.unknown.sideeffectsonly", constructorName(constructorElt));
      return super.visitNewClass(node, aVoid);
    }

    // The constructor modifies at most the expressions listed in its own `@SideEffectsOnly`
    // annotation.
    for (JavaExpression expr :
        constructorSideEffectedExpressions(node, constructorElt, seOnlyAnnotation)) {
      if (isDisallowedSideEffectedExpression(expr)) {
        disallowedSideEffects.add(IPair.of(node, expr));
      }
    }
    return super.visitNewClass(node, aVoid);
  }

  /**
   * Returns the expressions that the invoked constructor side-effects: the arguments/elements of
   * its {@link SideEffectsOnly} annotation, view-adapted to the given call site.
   *
   * <p>An expression that mentions {@code this} is omitted from the result. In a constructor's
   * annotation, {@code this} is the object being constructed, which did not exist before the call,
   * so modifying it is not a side effect that is visible to the caller.
   *
   * <p>If an expression cannot be parsed, this reports {@code purity.unknown.sideeffectsonly} and
   * returns an empty list, just as {@link #calleeSideEffectedExpressions} does.
   *
   * @param node a call to a constructor that is annotated with {@link SideEffectsOnly}
   * @param constructorElt the invoked constructor
   * @param seOnlyAnnotation the invoked constructor's {@link SideEffectsOnly} annotation
   * @return the expressions that the invoked constructor side-effects, view-adapted to {@code node}
   */
  protected List<JavaExpression> constructorSideEffectedExpressions(
      NewClassTree node, ExecutableElement constructorElt, AnnotationMirror seOnlyAnnotation) {
    List<String> exprStrings =
        AnnotationUtils.getElementValueArray(
            seOnlyAnnotation, sideEffectsOnlyValueElement, String.class);
    List<JavaExpression> result = new ArrayList<>(exprStrings.size());
    for (String exprString : exprStrings) {
      JavaExpression atDeclaration;
      try {
        atDeclaration = StringToJavaExpression.atMethodDecl(exprString, constructorElt, checker);
      } catch (JavaExpressionParseException ex) {
        // The parse error itself is reported at the constructor's declaration, by
        // BaseTypeVisitor.checkPurityAnnotations.
        checker.reportError(
            node, "purity.unknown.sideeffectsonly", constructorName(constructorElt));
        return Collections.emptyList();
      }
      if (atDeclaration.containedOfClass(ThisReference.class) != null) {
        // The expression is the object under construction, or is reached through it.
        continue;
      }
      result.add(atDeclaration.atConstructorInvocation(node));
    }
    return result;
  }

  /**
   * Returns a name for the given constructor, for use in a diagnostic message. The constructor's
   * own simple name is {@code <init>}, which would be unhelpful.
   *
   * @param constructorElt a constructor
   * @return the simple name of the class that the constructor constructs
   */
  private CharSequence constructorName(ExecutableElement constructorElt) {
    return constructorElt.getEnclosingElement().getSimpleName();
  }

  /**
   * Returns true if the given expression is a side-effected expression beyond what is listed in the
   * {@link SideEffectsOnly} annotation. That is, all of the following hold:
   *
   * <ul>
   *   <li>The expression's value is modifiable by other code.
   *   <li>The expression is not covered by the {@link SideEffectsOnly} annotation, in the sense of
   *       {@link #isCoveredByAnnotation}.
   * </ul>
   *
   * <p>Use this for an expression whose <em>value</em> is mutated, such as an expression that a
   * callee modifies. For an expression that is <em>assigned to</em>, use {@link
   * #isDisallowedAssignmentTarget}.
   *
   * @param expr the expression to check for side-effecting
   * @return true if the given expression is a side-effected expression beyond what is listed in the
   *     {@link SideEffectsOnly} annotation
   */
  protected boolean isDisallowedSideEffectedExpression(JavaExpression expr) {
    return expr.isModifiableByOtherCode() && !isCoveredByAnnotation(expr);
  }

  /**
   * Returns true if assigning to the given expression is a side effect beyond what is listed in the
   * {@link SideEffectsOnly} annotation. That is, all of the following hold:
   *
   * <ul>
   *   <li>The expression is assignable by other code; equivalently, the assignment is visible
   *       outside the method being checked. (Assigning to a local variable is not.)
   *   <li>The expression is not covered by the {@link SideEffectsOnly} annotation, in the sense of
   *       {@link #isCoveredByAnnotation}.
   * </ul>
   *
   * @param expr the expression that is assigned to
   * @return true if assigning to the given expression is a side effect beyond what is listed in the
   *     {@link SideEffectsOnly} annotation
   */
  protected boolean isDisallowedAssignmentTarget(JavaExpression expr) {
    return expr.isAssignableByOtherCode() && !isCoveredByAnnotation(expr);
  }

  /**
   * Returns true if the given expression is listed in the {@link SideEffectsOnly} annotation, is a
   * subexpression of one of those expressions, or may be aliased to one of them.
   *
   * @param expr the expression to look for
   * @return true if the given expression is covered by the {@link SideEffectsOnly} annotation
   */
  protected boolean isCoveredByAnnotation(JavaExpression expr) {
    aliasedExpressions.add(expr);
    for (JavaExpression seOnlyExpr : sideEffectsOnlyExpressionsFromAnnotation) {
      aliasedExpressions.add(seOnlyExpr);
      // Argument order matters: `test` lifts the asymmetric `containsAsReceiver` relation over
      // the two elements' alias sets, and `expr` must be the potential sub-expression.
      if (aliasedExpressions.test(expr, seOnlyExpr)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public Void visitAssignment(AssignmentTree node, Void aVoid) {
    JavaExpression lhs = JavaExpression.fromTree(node.getVariable());
    JavaExpression rhs = JavaExpression.fromTree(node.getExpression());
    if (isDisallowedAssignmentTarget(lhs)) {
      disallowedSideEffects.add(IPair.of(node, lhs));
    }
    aliasedExpressions.union(lhs, rhs);
    return super.visitAssignment(node, aVoid);
  }

  @Override
  public Void visitVariable(VariableTree node, Void aVoid) {
    ExpressionTree initializer = node.getInitializer();
    if (initializer == null) {
      // A declaration with no initializer, such as `int x;`, creates no alias.
      return super.visitVariable(node, aVoid);
    }
    JavaExpression name = JavaExpression.fromVariableTree(node);
    JavaExpression expr = JavaExpression.fromTree(initializer);
    // `union` adds both arguments, so they need not be added first.
    aliasedExpressions.union(name, expr);
    return super.visitVariable(node, aVoid);
  }

  @Override
  public Void visitUnary(UnaryTree node, Void aVoid) {
    switch (node.getKind()) {
      case POSTFIX_INCREMENT, POSTFIX_DECREMENT, PREFIX_INCREMENT, PREFIX_DECREMENT -> {
        JavaExpression operand = JavaExpression.fromTree(node.getExpression());
        if (isDisallowedAssignmentTarget(operand)) {
          disallowedSideEffects.add(IPair.of(node, operand));
        }
      }
      default -> {}
    }
    return super.visitUnary(node, aVoid);
  }

  @Override
  public Void visitCompoundAssignment(CompoundAssignmentTree node, Void aVoid) {
    // Does not make the left-hand side an alias of the right-hand side,
    // because the rhs expression uses the lhs.
    JavaExpression lhs = JavaExpression.fromTree(node.getVariable());
    if (isDisallowedAssignmentTarget(lhs)) {
      disallowedSideEffects.add(IPair.of(node, lhs));
    }
    return super.visitCompoundAssignment(node, aVoid);
  }
}
