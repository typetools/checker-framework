package org.checkerframework.dataflow.util;

import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.CatchTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ThrowTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import org.checkerframework.dataflow.qual.Deterministic;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.javacutil.AnnotationProvider;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreePathUtil;
import org.checkerframework.javacutil.TreeUtils;
import org.plumelib.util.IPair;

/**
 * A visitor that determines the purity (as defined by {@link
 * org.checkerframework.dataflow.qual.SideEffectFree}, {@link
 * org.checkerframework.dataflow.qual.Deterministic}, and {@link
 * org.checkerframework.dataflow.qual.Pure}) of a statement or expression. The entry point is method
 * {@link #checkPurity}.
 *
 * @see SideEffectFree
 * @see Deterministic
 * @see Pure
 */
public class PurityChecker {

  /**
   * Compute whether the given statement is side-effect-free, deterministic, or both. Returns a
   * result that can be queried.
   *
   * @param statement the statement to check
   * @param annoProvider the annotation provider
   * @param assumeSideEffectFree true if all methods should be assumed to be @SideEffectFree
   * @param assumeDeterministic true if all methods should be assumed to be @Deterministic
   * @param assumePureGetters true if all getter methods should be assumed to be @Pure
   * @return information about whether the given statement is side-effect-free, deterministic, or
   *     both
   */
  public static PurityResult checkPurity(
      TreePath statement,
      AnnotationProvider annoProvider,
      boolean assumeSideEffectFree,
      boolean assumeDeterministic,
      boolean assumePureGetters) {
    PurityCheckerHelper helper =
        new PurityCheckerHelper(
            annoProvider, assumeSideEffectFree, assumeDeterministic, assumePureGetters);
    helper.scan(statement, null);
    return helper.purityResult;
  }

  /**
   * Result of the {@link PurityChecker}. Can be queried regarding whether a given tree was
   * side-effect-free, deterministic, or both; also gives reasons if the answer is "no".
   */
  public static class PurityResult {

    /** Reasons that the referenced method is not side-effect-free. */
    protected final List<IPair<Tree, String>> notSEFreeReasons = new ArrayList<>(1);

    /** Reasons that the referenced method is not deterministic. */
    protected final List<IPair<Tree, String>> notDetReasons = new ArrayList<>(1);

    /** Reasons that the referenced method is not side-effect-free and deterministic. */
    protected final List<IPair<Tree, String>> notBothReasons = new ArrayList<>(1);

    /**
     * Contains all the varieties of purity that the expression has. Starts out with all varieties,
     * and elements are removed from it as violations are found.
     */
    protected EnumSet<Pure.Kind> kinds = EnumSet.allOf(Pure.Kind.class);

    /**
     * Return the kinds of purity that the method has.
     *
     * @return the kinds of purity that the method has
     */
    public EnumSet<Pure.Kind> getKinds() {
      return kinds;
    }

    /**
     * Is the method pure w.r.t. a given set of kinds?
     *
     * @param otherKinds the varieties of purity to check
     * @return true if the method is pure with respect to all the given kinds
     */
    public boolean isPure(EnumSet<Pure.Kind> otherKinds) {
      return kinds.containsAll(otherKinds);
    }

    /**
     * Get the reasons why the method is not side-effect-free.
     *
     * @return the reasons why the method is not side-effect-free
     */
    public List<IPair<Tree, String>> getNotSEFreeReasons() {
      return notSEFreeReasons;
    }

    /**
     * Add a reason why the method is not side-effect-free.
     *
     * @param t a tree
     * @param msgId why the tree is not side-effect-free
     */
    public void addNotSEFreeReason(Tree t, String msgId) {
      notSEFreeReasons.add(IPair.of(t, msgId));
      kinds.remove(Pure.Kind.SIDE_EFFECT_FREE);
    }

    /**
     * Get the reasons why the method is not deterministic.
     *
     * @return the reasons why the method is not deterministic
     */
    public List<IPair<Tree, String>> getNotDetReasons() {
      return notDetReasons;
    }

    /**
     * Add a reason why the method is not deterministic.
     *
     * @param t a tree
     * @param msgId why the tree is not deterministic
     */
    public void addNotDetReason(Tree t, String msgId) {
      notDetReasons.add(IPair.of(t, msgId));
      kinds.remove(Pure.Kind.DETERMINISTIC);
    }

    /**
     * Get the reasons why the method is not both side-effect-free and deterministic.
     *
     * @return the reasons why the method is not both side-effect-free and deterministic
     */
    public List<IPair<Tree, String>> getNotBothReasons() {
      return notBothReasons;
    }

    /**
     * Add a reason why the method is not both side-effect-free and deterministic.
     *
     * @param t tree
     * @param msgId why the tree is not deterministic and side-effect-free
     */
    public void addNotBothReason(Tree t, String msgId) {
      notBothReasons.add(IPair.of(t, msgId));
      kinds.remove(Pure.Kind.DETERMINISTIC);
      kinds.remove(Pure.Kind.SIDE_EFFECT_FREE);
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
     * @param assumeSideEffectFree true if all methods should be assumed to be @SideEffectFree
     * @param assumeDeterministic true if all methods should be assumed to be @Deterministic
     * @param assumePureGetters true if getter methods should be assumed to be @Pure
     */
    public PurityCheckerHelper(
        AnnotationProvider annoProvider,
        boolean assumeSideEffectFree,
        boolean assumeDeterministic,
        boolean assumePureGetters) {
      this.annoProvider = annoProvider;
      this.assumeSideEffectFree = assumeSideEffectFree;
      this.assumeDeterministic = assumeDeterministic;
      this.assumePureGetters = assumePureGetters;
    }

    @Override
    public Void visitCatch(CatchTree tree, Void ignore) {
      purityResult.addNotDetReason(tree, "catch");
      return super.visitCatch(tree, ignore);
    }

    /** Represents a method that is both deterministic and side-effect free. */
    private static final EnumSet<Pure.Kind> detAndSeFree =
        EnumSet.of(Pure.Kind.DETERMINISTIC, Pure.Kind.SIDE_EFFECT_FREE);

    @Override
    public Void visitMethodInvocation(MethodInvocationTree tree, Void ignore) {
      ExecutableElement elt = TreeUtils.elementFromUse(tree);
      if (!PurityUtils.hasPurityAnnotation(annoProvider, elt)) {
        purityResult.addNotBothReason(tree, "call");
      } else {
        EnumSet<Pure.Kind> purityKinds =
            ((assumeDeterministic && assumeSideEffectFree)
                    || (assumePureGetters && ElementUtils.isGetter(elt)))
                // Avoid computation if not necessary
                ? detAndSeFree
                : PurityUtils.getPurityKinds(annoProvider, elt);
        boolean det = assumeDeterministic || purityKinds.contains(Pure.Kind.DETERMINISTIC);
        boolean seFree = assumeSideEffectFree || purityKinds.contains(Pure.Kind.SIDE_EFFECT_FREE);
        if (!det && !seFree) {
          purityResult.addNotBothReason(tree, "call");
        } else if (!det) {
          purityResult.addNotDetReason(tree, "call");
        } else if (!seFree) {
          purityResult.addNotSEFreeReason(tree, "call");
        }
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
      //  * permit other non-deterministic expresssions within throw (at which time move this
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
      // because the constructor is called at all, and the other is because the constuctor is
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
        case POSTFIX_DECREMENT:
        case POSTFIX_INCREMENT:
        case PREFIX_DECREMENT:
        case PREFIX_INCREMENT:
          ExpressionTree expression = tree.getExpression();
          assignmentCheck(expression);
          break;
        default:
          // Nothing to do
          break;
      }
      return super.visitUnary(tree, ignore);
    }

    /**
     * Check whether {@code variable} is permitted on the left-hand-side of an assignment.
     *
     * @param variable the lhs to check
     */
    protected void assignmentCheck(ExpressionTree variable) {
      variable = TreeUtils.withoutParens(variable);
      VariableElement fieldElt = TreeUtils.asFieldAccess(variable);
      if (fieldElt != null
          && isFieldInCurrentClass(fieldElt)
          && TreePathUtil.inConstructor(getCurrentPath())) {
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
