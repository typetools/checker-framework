package org.checkerframework.dataflow.util;

import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.CatchTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ThrowTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.TreeScanner;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import org.checkerframework.checker.nullness.qual.Nullable;
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
      kinds.remove(PurityKind.SIDE_EFFECT_FREE);
    }

    /**
     * Returns the reasons why the method is not deterministic.
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
      kinds.remove(PurityKind.DETERMINISTIC);
    }

    /**
     * Returns the reasons why the method is not both side-effect-free and deterministic.
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

    /**
     * Caches the results of {@link #ownedArrayDepth}, which requires scanning a whole class
     * declaration.
     */
    private final Map<VariableElement, Integer> ownedArrayDepths = new HashMap<>(2);

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
    private static final EnumSet<PurityKind> detAndSeFree =
        EnumSet.of(PurityKind.DETERMINISTIC, PurityKind.SIDE_EFFECT_FREE);

    @Override
    public Void visitMethodInvocation(MethodInvocationTree tree, Void ignore) {
      ExecutableElement elt = TreeUtils.elementFromUse(tree);
      EnumSet<PurityKind> eltPurityKinds = PurityUtils.getPurityKinds(annoProvider, elt);
      if (!eltPurityKinds.contains(PurityKind.SIDE_EFFECT_FREE)
          && !eltPurityKinds.contains(PurityKind.DETERMINISTIC)) {
        // The called method has no purity annotation, so the callee is not pure either.
        purityResult.addNotBothReason(tree, "call");
      } else {
        // The called method has a purity annotation:  @SideEffectFree, @Deterministic, or both.
        EnumSet<PurityKind> purityKinds =
            ((assumeDeterministic && assumeSideEffectFree)
                    || (assumePureGetters && ElementUtils.isGetter(elt)))
                // Avoid computation if not necessary
                ? detAndSeFree
                : eltPurityKinds;
        boolean det =
            assumeDeterministic
                || purityKinds.contains(PurityKind.DETERMINISTIC)
                || elt.getReturnType().getKind() == TypeKind.VOID;
        boolean seFree = assumeSideEffectFree || purityKinds.contains(PurityKind.SIDE_EFFECT_FREE);
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
      if (TreePathUtil.inConstructor(getCurrentPath()) && writesFieldInCurrentClass(variable, 0)) {
        // assigning a field of the object being constructed, or an element of an array that such
        // a field owns
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
     * Returns true if writing the given expression writes a field of the current class, or an
     * element of an array that such a field owns (possibly nested, as in {@code f[0][1]}).
     *
     * <p>Writing an array element counts as writing the field that holds the array only if no code
     * outside the object under construction can have a reference to the array: the field is a
     * non-static field of {@code this}, it is only ever assigned freshly-created arrays, and its
     * value never escapes. Otherwise the write is visible to code that shares the array, as when a
     * constructor stores its argument in the field and then writes an element of it.
     *
     * @param variable the left-hand side of an assignment, or the array of an array access that it
     *     is nested within
     * @param depth the number of array accesses that have been stripped from the left-hand side; 0
     *     when {@code variable} is the left-hand side itself
     * @return true if the assignment writes a field of the current class or an element of an array
     *     that such a field owns
     */
    private boolean writesFieldInCurrentClass(ExpressionTree variable, int depth) {
      variable = TreeUtils.withoutParens(variable);
      if (variable instanceof ArrayAccessTree arrayAccess) {
        return writesFieldInCurrentClass(arrayAccess.getExpression(), depth + 1);
      }
      VariableElement fieldElt = TreeUtils.asFieldAccess(variable);
      if (fieldElt == null || !isFieldInCurrentClass(fieldElt)) {
        return false;
      }
      if (depth == 0) {
        // The field itself is assigned, not an element of an array that it holds.
        return true;
      }
      // A static field is not part of the object under construction, and a field of another
      // object is that object's, so neither one owns its array.
      return !ElementUtils.isStatic(fieldElt)
          && isAccessOfThis(variable)
          && depth <= ownedArrayDepth(fieldElt);
    }

    /**
     * Returns true if the given field access reads a field of {@code this}, rather than of some
     * other object.
     *
     * @param fieldAccess a field access, without parentheses
     * @return true if the field access is on {@code this}
     */
    private boolean isAccessOfThis(ExpressionTree fieldAccess) {
      if (fieldAccess instanceof MemberSelectTree memberSelect) {
        return TreeUtils.isExplicitThisDereference(memberSelect.getExpression());
      }
      // An unqualified reference to a field of the current class is a reference to a field of
      // `this`.
      return true;
    }

    /**
     * Returns the number of levels of indexing for which the array in the given field is owned by
     * the object under construction: that is, the depth to which every array reachable from the
     * field was created within the class and cannot be reached by any other code. The result is 0
     * if the field's value might be aliased.
     *
     * <p>For example, the result is 0 for a field that a constructor assigns from its argument, 1
     * for a field assigned {@code new int[][] {arg1, arg2}}, and unbounded for a field only ever
     * assigned {@code new int[2][2]}.
     *
     * <p>This assumes that no other code observes the object while its constructor runs, which
     * holds because a constructor that leaks {@code this} is not side-effect-free.
     *
     * @param fieldElt a field of the current class
     * @return the number of levels of indexing under which writes to the field's array cannot be
     *     observed by other code
     */
    private int ownedArrayDepth(VariableElement fieldElt) {
      return ownedArrayDepths.computeIfAbsent(
          fieldElt,
          f -> {
            // Scan the outermost class, because the field may be used anywhere within it.
            ClassTree outermostClass = null;
            for (TreePath p = getCurrentPath(); p != null; p = p.getParentPath()) {
              if (p.getLeaf() instanceof ClassTree classTree) {
                outermostClass = classTree;
              }
            }
            if (outermostClass == null) {
              return 0;
            }
            OwnedArrayScanner scanner = new OwnedArrayScanner(f);
            scanner.scan(outermostClass, null);
            return scanner.depth;
          });
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

  /**
   * Scans a class declaration to determine how deeply the array in a given field is owned by the
   * object that holds it: to what depth every array reachable from the field is created within the
   * class and no other code can obtain a reference to it.
   *
   * <p>The array's depth is limited by every assignment that stores a value that might be aliased,
   * and is 0 if the field's value is ever used as a whole, since it could then be stored anywhere.
   * Indexing the array and reading its length do not give out a reference to it.
   */
  private static class OwnedArrayScanner extends TreeScanner<Void, Void> {

    /** The field whose uses to examine. */
    private final VariableElement field;

    /**
     * The number of levels of indexing for which the field's array is owned, given the uses seen so
     * far. {@link Integer#MAX_VALUE} means "any depth".
     */
    private int depth = Integer.MAX_VALUE;

    /**
     * Creates an OwnedArrayScanner.
     *
     * @param field the field whose uses to examine
     */
    OwnedArrayScanner(VariableElement field) {
      this.field = field;
    }

    /**
     * Returns true if the given expression is an access to {@link #field}, of any object.
     *
     * @param tree an expression, without parentheses
     * @return true if the expression is an access to {@link #field}
     */
    private boolean isAccessOfField(ExpressionTree tree) {
      VariableElement accessed = TreeUtils.asFieldAccess(tree);
      return accessed != null && field.equals(accessed);
    }

    /**
     * Scans the receiver of a field access, which is not itself a use of the field.
     *
     * @param fieldAccess an access to {@link #field}, without parentheses
     * @return null
     */
    private Void scanReceiver(ExpressionTree fieldAccess) {
      if (fieldAccess instanceof MemberSelectTree memberSelect) {
        return scan(memberSelect.getExpression(), null);
      }
      return null;
    }

    /**
     * Records that the array {@code indices} levels down from the field is assigned the given
     * value.
     *
     * @param value the assigned value, or null if a field declaration has no initializer
     * @param indices the number of array accesses through which the value is stored
     */
    private void assigned(@Nullable ExpressionTree value, int indices) {
      int fresh = freshDepth(value);
      depth = Math.min(depth, fresh == Integer.MAX_VALUE ? Integer.MAX_VALUE : indices + fresh);
    }

    /**
     * Returns the number of levels of indexing for which the given expression is known to evaluate
     * to a newly-created array that no other code can reach; 0 if it might be an alias of an array
     * that other code holds.
     *
     * @param value an expression, or null if a field declaration has no initializer
     * @return the depth to which the expression's value is freshly created
     */
    private static int freshDepth(@Nullable ExpressionTree value) {
      if (value == null) {
        // A declaration without an initializer stores nothing; the assignments that do store a
        // value are examined separately.
        return Integer.MAX_VALUE;
      }
      if (!(TreeUtils.withoutParens(value) instanceof NewArrayTree newArray)) {
        return 0;
      }
      List<? extends ExpressionTree> initializers = newArray.getInitializers();
      if (initializers == null) {
        // "new int[2][2]" creates every array within it.
        return Integer.MAX_VALUE;
      }
      // The array itself is fresh, so its elements may be replaced; each element is fresh only as
      // deeply as the expression that produced it.
      int result = Integer.MAX_VALUE;
      for (ExpressionTree initializer : initializers) {
        int fresh = freshDepth(initializer);
        if (fresh != Integer.MAX_VALUE) {
          result = Math.min(result, fresh + 1);
        }
      }
      return result;
    }

    @Override
    public Void visitVariable(VariableTree tree, Void ignore) {
      VariableElement declared = TreeUtils.elementFromDeclaration(tree);
      if (declared != null && field.equals(declared)) {
        assigned(tree.getInitializer(), 0);
      }
      return super.visitVariable(tree, ignore);
    }

    @Override
    public Void visitAssignment(AssignmentTree tree, Void ignore) {
      // Strip the array accesses, if any, to find what the assignment ultimately writes into.
      ExpressionTree target = TreeUtils.withoutParens(tree.getVariable());
      int indices = 0;
      while (target instanceof ArrayAccessTree arrayAccess) {
        indices++;
        scan(arrayAccess.getIndex(), ignore);
        target = TreeUtils.withoutParens(arrayAccess.getExpression());
      }
      if (isAccessOfField(target)) {
        assigned(tree.getExpression(), indices);
        scanReceiver(target);
      } else {
        scan(target, ignore);
      }
      return scan(tree.getExpression(), ignore);
    }

    @Override
    public Void visitArrayAccess(ArrayAccessTree tree, Void ignore) {
      ExpressionTree array = TreeUtils.withoutParens(tree.getExpression());
      if (isAccessOfField(array)) {
        // Indexing the array does not give out a reference to it.
        scanReceiver(array);
        return scan(tree.getIndex(), ignore);
      }
      return super.visitArrayAccess(tree, ignore);
    }

    @Override
    public Void visitMemberSelect(MemberSelectTree tree, Void ignore) {
      ExpressionTree receiver = TreeUtils.withoutParens(tree.getExpression());
      if (tree.getIdentifier().contentEquals("length") && isAccessOfField(receiver)) {
        // Reading the length does not give out a reference to the array.
        return scanReceiver(receiver);
      }
      if (isAccessOfField(tree)) {
        // The field's value is used as a whole, so it might be stored anywhere.
        depth = 0;
        return scanReceiver(tree);
      }
      return super.visitMemberSelect(tree, ignore);
    }

    @Override
    public Void visitIdentifier(IdentifierTree tree, Void ignore) {
      if (isAccessOfField(tree)) {
        // The field's value is used as a whole, so it might be stored anywhere.
        depth = 0;
      }
      return null;
    }
  }
}
