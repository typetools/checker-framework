package org.checkerframework.checker.resourceleak;

import com.sun.source.tree.AssertTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.CatchTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.EmptyStatementTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.LabeledStatementTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TryTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreeScanner;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import org.checkerframework.checker.interning.qual.FindDistinct;
import org.checkerframework.checker.interning.qual.InternedDistinct;
import org.checkerframework.checker.rlccalledmethods.RLCCalledMethodsAnnotatedTypeFactory;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.TreePathUtil;
import org.checkerframework.javacutil.TreeUtils;

/**
 * Conservatively check whether a constructor assignment is the first write to its field.
 *
 * <p>The entry point is {@link #isFirstWriteToFieldInConstructor(Tree, VariableElement, MethodTree,
 * RLCCalledMethodsAnnotatedTypeFactory)}.
 */
final class ConstructorFirstWriteAnalysis {

  /** Do not instantiate. */
  private ConstructorFirstWriteAnalysis() {
    throw new Error("Do not instantiate");
  }

  /**
   * Returns true if the given assignment is the first write to {@code targetField} on its path in
   * the constructor. A true return value means that <em>if</em> the assignment is executed, then it
   * is the first assignment to the field. This method is conservative: it returns {@code false}
   * unless it can prove that the write is the first.
   *
   * <p>It returns {@code true} only if all the following hold:
   *
   * <ul>
   *   <li>(1) The field has no non-null inline initializer at its declaration.
   *   <li>(2) The field is not assigned in any instance initializer block.
   *   <li>(3) An AST scan of the constructor body does not encounter an earlier write to the same
   *       field or a disqualifying side effect before reaching the target assignment.
   * </ul>
   *
   * Don't use this for final fields, because the Java compiler already guarantees they are assigned
   * exactly once.
   *
   * @param assignment the assignment tree being analyzed, which is a statement in the body of
   *     {@code constructor}
   * @param targetField the non-final private field assigned by {@code assignment}; its type is
   *     non-primitive
   * @param constructor the constructor where {@code assignment} appears
   * @param cmAtf the factory used for side-effect reasoning and tree-path lookup
   * @return true if this assignment is definitely the first write during construction, or false if
   *     that fact cannot be established
   */
  static boolean isFirstWriteToFieldInConstructor(
      @FindDistinct Tree assignment,
      @FindDistinct VariableElement targetField,
      MethodTree constructor,
      RLCCalledMethodsAnnotatedTypeFactory cmAtf) {
    TreePath constructorPath = cmAtf.getPath(constructor);
    if (constructorPath == null) {
      throw new BugInCF("Constructor has no tree path: %s", constructor);
    }
    ClassTree classTree = TreePathUtil.enclosingClass(constructorPath);
    if (classTree == null) {
      throw new BugInCF("Constructor has no enclosing class: %s", constructor);
    }
    // Final fields should not reach this helper, because they cannot be reassigned.
    if (targetField.getModifiers().contains(Modifier.FINAL)) {
      throw new BugInCF("Target field is final: %s", targetField);
    }

    // (1) and (2): if the field has non-null inline initializer or is assigned in an instance
    // initializer block, then the constructor assignment is not the first write.
    if (mayBeAssignedInInitializer(classTree, targetField, cmAtf)) {
      return false;
    }

    // (3): Single-pass conservative scan of the constructor body.
    FirstWriteScanner scanner = new FirstWriteScanner(assignment, targetField, cmAtf);
    FirstWriteScanResult r = scanner.scan(constructor.getBody(), null);
    return r == FirstWriteScanResult.FIRST_ASSIGNMENT;
  }

  /**
   * Returns true if {@code classTree} contains a non-null field initializer or an instance
   * initializer block that may assign {@code targetField}.
   *
   * @param classTree the class to inspect
   * @param targetField the field being checked
   * @param cmAtf the factory used for side-effect reasoning
   * @return true if {@code targetField} may be assigned in a field initializer or instance
   *     initializer block in {@code classTree}
   */
  private static boolean mayBeAssignedInInitializer(
      ClassTree classTree,
      @FindDistinct VariableElement targetField,
      RLCCalledMethodsAnnotatedTypeFactory cmAtf) {
    for (Tree member : classTree.getMembers()) {
      // Look for a non-null inline initializer on the field declaration.
      if (member instanceof VariableTree decl) {
        VariableElement declElement = TreeUtils.elementFromDeclaration(decl);
        if (targetField == declElement
            && decl.getInitializer() != null
            && decl.getInitializer().getKind() != Tree.Kind.NULL_LITERAL) {
          return true;
        }
        continue;
      }

      // Assignment in any instance initializer block.
      if (member instanceof BlockTree initBlock) {
        if (initBlock.isStatic()) {
          continue;
        }
        if (BlockAssignmentScanner.mayBeAssigned(initBlock, targetField, cmAtf)) {
          return true;
        }
      }
    }
    return false;
  }

  /** Result of scanning a tree region for the target assignment. */
  private enum FirstWriteScanResult {
    /** The target assignment is definitely the first assignment in the scanned region. */
    FIRST_ASSIGNMENT,
    /**
     * The target assignment might be a reassignment. It is disqualified by an earlier write,
     * disallowed call, or unsupported statement form.
     */
    REASSIGNMENT,
    /** No assignment to the target field occurs in the scanned region. */
    UNASSIGNED
  }

  /**
   * Scanner that checks whether a block may assign the target field.
   *
   * <p>The entry point is {@link #mayBeAssigned}. It returns true if the given block either assigns
   * the field directly or may do so through a side-effecting method call or object creation.
   */
  private static final class BlockAssignmentScanner extends BooleanShortCircuitScanner {

    /** The field being analyzed. */
    private final @InternedDistinct VariableElement targetField;

    /** The annotated type factory, used to determine whether a method has any side effects. */
    private final RLCCalledMethodsAnnotatedTypeFactory cmAtf;

    /**
     * Creates a scanner that checks whether a block may assign {@code targetField}.
     *
     * @param targetField the field being checked
     * @param cmAtf the type factory for side-effect reasoning
     */
    private BlockAssignmentScanner(
        @FindDistinct VariableElement targetField, RLCCalledMethodsAnnotatedTypeFactory cmAtf) {
      this.targetField = targetField;
      this.cmAtf = cmAtf;
    }

    /**
     * Returns true if {@code block} directly assigns {@code targetField} or may assign it through a
     * side-effecting call or object creation.
     *
     * @param block the block to scan
     * @param targetField the field under analyze
     * @param cmAtf the factory used for side-effect reasoning
     * @return true if {@code block} may assign {@code targetField}
     */
    static boolean mayBeAssigned(
        BlockTree block,
        @FindDistinct VariableElement targetField,
        RLCCalledMethodsAnnotatedTypeFactory cmAtf) {
      return new BlockAssignmentScanner(targetField, cmAtf).scan(block, null);
    }

    @Override
    public Boolean visitAssignment(AssignmentTree node, Void p) {
      Element assignedElement = TreeUtils.elementFromTree(node.getVariable());
      if (targetField == assignedElement) {
        return true;
      }
      return super.visitAssignment(node, p);
    }

    @Override
    public Boolean visitMethodInvocation(MethodInvocationTree node, Void p) {
      // Any side-effecting method call in a block could write to the field.
      if (!cmAtf.isSideEffectFree(TreeUtils.elementFromUse(node))) {
        return true;
      }
      return super.visitMethodInvocation(node, p);
    }

    @Override
    public Boolean visitNewClass(NewClassTree node, Void p) {
      if (!cmAtf.isSideEffectFree(TreeUtils.elementFromUse(node))) {
        return true;
      }
      return super.visitNewClass(node, p);
    }
  }

  /**
   * Scanner that scans a tree fragment to determine whether a given assignment is definitely the
   * first write to its field before any earlier assignment or side-effecting call in that fragment.
   *
   * <p>This scanner reasons only about the scanned tree fragment. It does not handle field
   * initializers or instance initializer blocks, which are checked separately by {@link
   * #mayBeAssignedInInitializer(ClassTree, VariableElement, RLCCalledMethodsAnnotatedTypeFactory)}.
   *
   * <p>This scanner is conservative and does not model every tree kind. Supported statement kinds
   * are documented in {@link FirstWriteScanner#isModeledStatementKind(Tree.Kind)}. Unmodelled
   * statement kinds conservatively return {@link FirstWriteScanResult#REASSIGNMENT}.
   */
  private static final class FirstWriteScanner extends TreeScanner<FirstWriteScanResult, Void> {

    /** The assignment being analyzed. */
    private final @InternedDistinct Tree targetAssignment;

    /** The field assigned by {@code targetAssignment}. */
    private final @InternedDistinct VariableElement targetField;

    /** The annotated type factory, used to determine whether a method has any side effects. */
    private final RLCCalledMethodsAnnotatedTypeFactory cmAtf;

    /**
     * Creates a scanner that checks if {@code targetAssignment} is the first write to {@code
     * targetField} in the scanned tree. The scan stops as soon as a decisive result is encountered.
     *
     * @param targetAssignment the assignment being analyzed
     * @param targetField the field written by that assignment
     * @param cmAtf the type factory for side-effect reasoning
     */
    private FirstWriteScanner(
        @FindDistinct Tree targetAssignment,
        @FindDistinct VariableElement targetField,
        RLCCalledMethodsAnnotatedTypeFactory cmAtf) {
      this.targetAssignment = targetAssignment;
      this.targetField = targetField;
      this.cmAtf = cmAtf;
    }

    /**
     * Returns true if this scanner explicitly models statement kind {@code kind}.
     *
     * @param kind the statement kind
     * @return {@code true} if this scanner models {@code kind}, or {@code false} if it
     *     conservatively treats it as {@link FirstWriteScanResult#REASSIGNMENT}
     */
    private static boolean isModeledStatementKind(Tree.Kind kind) {
      return switch (kind) {
        case BLOCK,
            EXPRESSION_STATEMENT,
            VARIABLE,
            TRY,
            IF,
            ASSERT,
            EMPTY_STATEMENT,
            LABELED_STATEMENT,
            RETURN ->
            true;
        default -> false;
      };
    }

    @Override
    public FirstWriteScanResult visitBlock(BlockTree node, Void p) {
      return scan(node.getStatements(), p);
    }

    @Override
    public FirstWriteScanResult visitExpressionStatement(ExpressionStatementTree node, Void p) {
      return scan(node.getExpression(), p);
    }

    @Override
    public FirstWriteScanResult visitVariable(VariableTree node, Void p) {
      // The compiler has already desugared a declaration with multiple variables
      // (e.g., int a = 1, b = 2;) into multiple independent VariableTree nodes.
      return scan(node.getInitializer(), p);
    }

    @Override
    public FirstWriteScanResult visitTry(TryTree node, Void p) {
      // Evaluate resource initializers before the try block. This is not sound as the implicit
      // close() calls are not represented in the AST.
      for (Tree resource : node.getResources()) {
        FirstWriteScanResult res = scan(resource, p);
        if (res != FirstWriteScanResult.UNASSIGNED) {
          return res;
        }
      }

      // Scan the try block first before any catch or finally block.
      FirstWriteScanResult res = scan(node.getBlock(), p);
      if (res != FirstWriteScanResult.UNASSIGNED) {
        return res;
      }

      // Scan the catch blocks one by one, and merge their results as only one catch block can
      // execute.
      // If any catch block results to FIRST_ASSIGNMENT, then the target assignment is the first
      // write for the try tree.
      // Otherwise, if any catch block results to REASSIGNMENT, then the try tree results to
      // REASSIGNMENT.
      FirstWriteScanResult catchesRes = FirstWriteScanResult.UNASSIGNED;
      for (CatchTree catchTree : node.getCatches()) {
        FirstWriteScanResult catchRes = scan(catchTree.getBlock(), p);
        if (catchRes == FirstWriteScanResult.FIRST_ASSIGNMENT) {
          return FirstWriteScanResult.FIRST_ASSIGNMENT;
        }
        if (catchRes == FirstWriteScanResult.REASSIGNMENT) {
          catchesRes = FirstWriteScanResult.REASSIGNMENT;
        }
      }
      if (catchesRes != FirstWriteScanResult.UNASSIGNED) {
        return catchesRes;
      }

      // If catch blocks are UNASSIGNED, then continue to scan the finally block.
      BlockTree finallyBlock = node.getFinallyBlock();
      if (finallyBlock != null) {
        return scan(finallyBlock, p);
      }

      return FirstWriteScanResult.UNASSIGNED;
    }

    @Override
    public FirstWriteScanResult visitIf(IfTree node, Void p) {
      // Scan the condition first and return any decisive result
      FirstWriteScanResult condRes = scan(node.getCondition(), p);
      if (condRes != FirstWriteScanResult.UNASSIGNED) {
        return condRes;
      }

      StatementTree thenStmt = node.getThenStatement();
      StatementTree elseStmt = node.getElseStatement();

      // Scan the `then` and the `else` branch independently to merge their results.
      FirstWriteScanResult thenRes = scan(thenStmt, p);
      // The else branch may not exist
      FirstWriteScanResult elseRes =
          elseStmt == null ? FirstWriteScanResult.UNASSIGNED : scan(elseStmt, p);

      // A FIRST_ASSIGNMENT in either branch means the target assignment is the first write for
      // the if statement.
      if (thenRes == FirstWriteScanResult.FIRST_ASSIGNMENT
          || elseRes == FirstWriteScanResult.FIRST_ASSIGNMENT) {
        return FirstWriteScanResult.FIRST_ASSIGNMENT;
      }
      // If neither branch yields FIRST_ASSIGNMENT, then a REASSIGNMENT in either branch is
      // the result of the if statement.
      if (thenRes == FirstWriteScanResult.REASSIGNMENT
          || elseRes == FirstWriteScanResult.REASSIGNMENT) {
        return FirstWriteScanResult.REASSIGNMENT;
      }
      return FirstWriteScanResult.UNASSIGNED;
    }

    @Override
    public FirstWriteScanResult visitAssignment(AssignmentTree node, Void p) {
      // Scan the LHS for side-effecting-calls, e.g., sideEffect().f = ...;
      FirstWriteScanResult lhsRes = scan(node.getVariable(), p);
      if (lhsRes != FirstWriteScanResult.UNASSIGNED) {
        return lhsRes;
      }
      // Scan the RHS to catch nested assignments, e.g., ... = (this.f = new Foo()).
      FirstWriteScanResult rhsRes = scan(node.getExpression(), p);
      if (rhsRes != FirstWriteScanResult.UNASSIGNED) {
        return rhsRes;
      }
      Element lhsEl = TreeUtils.elementFromUse(node.getVariable());
      if (targetField == lhsEl) {
        // Found an assignment to the same field:
        //   - current assignment → FIRST_ASSIGNMENT
        //   - different assignment → REASSIGNMENT
        return node == targetAssignment
            ? FirstWriteScanResult.FIRST_ASSIGNMENT
            : FirstWriteScanResult.REASSIGNMENT;
      }
      return FirstWriteScanResult.UNASSIGNED;
    }

    @Override
    public FirstWriteScanResult visitMethodInvocation(MethodInvocationTree node, Void p) {
      // Treat any side-effecting call before the target assignment as possibly assigning the field.
      // An explicit super(...) call is also allowed. This is unsound, because a superclass
      // constructor can invoke overridable code that writes the field. The special case keeps
      // explicit super(...) consistent with the implicit superclass-constructor call, which does
      // not appear in the AST.
      if (cmAtf.isSideEffectFree(TreeUtils.elementFromUse(node))
          || TreeUtils.isSuperConstructorCall(node)) {
        return super.visitMethodInvocation(node, p);
      }
      return FirstWriteScanResult.REASSIGNMENT;
    }

    @Override
    public FirstWriteScanResult visitNewClass(NewClassTree node, Void p) {
      // An object creation with side effects can modify the target field (e.g., via Helper(this),
      // where `Helper` can mutate `this`).
      if (cmAtf.isSideEffectFree(TreeUtils.elementFromUse(node))) {
        return super.visitNewClass(node, p);
      }
      return FirstWriteScanResult.REASSIGNMENT;
    }

    @Override
    public FirstWriteScanResult visitAssert(AssertTree node, Void p) {
      FirstWriteScanResult condRes = scan(node.getCondition(), p);
      if (condRes != FirstWriteScanResult.UNASSIGNED) {
        return condRes;
      }
      return scan(node.getDetail(), p);
    }

    @Override
    public FirstWriteScanResult visitEmptyStatement(EmptyStatementTree node, Void p) {
      return FirstWriteScanResult.UNASSIGNED;
    }

    @Override
    public FirstWriteScanResult visitLabeledStatement(LabeledStatementTree node, Void p) {
      return scan(node.getStatement(), p);
    }

    @Override
    public FirstWriteScanResult visitReturn(ReturnTree node, Void p) {
      return scan(node.getExpression(), p);
    }

    @Override
    public FirstWriteScanResult scan(Tree tree, Void p) {
      if (tree == null) {
        return FirstWriteScanResult.UNASSIGNED;
      }
      if (tree instanceof StatementTree && !isModeledStatementKind(tree.getKind())) {
        // Conservatively reject unmodeled statement kinds.
        return FirstWriteScanResult.REASSIGNMENT;
      }
      FirstWriteScanResult result = super.scan(tree, p);
      return result == null ? FirstWriteScanResult.UNASSIGNED : result;
    }

    @Override
    public FirstWriteScanResult scan(Iterable<? extends Tree> trees, Void p) {
      for (Tree tree : trees) {
        FirstWriteScanResult result = scan(tree, p);
        if (result != FirstWriteScanResult.UNASSIGNED) {
          return result;
        }
      }
      return FirstWriteScanResult.UNASSIGNED;
    }

    @Override
    public FirstWriteScanResult reduce(FirstWriteScanResult r1, FirstWriteScanResult r2) {
      // Preserve the first decisive result found among the children.
      if (r1 != FirstWriteScanResult.UNASSIGNED) {
        return r1;
      }
      return r2;
    }
  }

  /**
   * Tree scanner that returns {@code true} as soon as a scanned subtree returns {@code true}. This
   * class treats {@code null} results as {@code false}, combines child results using logical OR,
   * and stops scanning sibling trees after the first {@code true} result.
   *
   * <p>Subclasses define the matching condition in visit methods and return {@code true} when they
   * find a match.
   *
   * <p>This scanner is only used in {@link BlockAssignmentScanner}, but keeping it separate to
   * allow it to be reused for other similar checks in the future.
   */
  private abstract static class BooleanShortCircuitScanner extends TreeScanner<Boolean, Void> {

    /** Creates a {@link BooleanShortCircuitScanner}. */
    private BooleanShortCircuitScanner() {}

    @Override
    public Boolean scan(Tree tree, Void p) {
      if (tree == null) {
        return false;
      }
      Boolean result = super.scan(tree, p);
      return result != null && result;
    }

    @Override
    public Boolean scan(Iterable<? extends Tree> trees, Void p) {
      for (Tree tree : trees) {
        if (scan(tree, p)) {
          return true;
        }
      }
      return false;
    }

    @Override
    public Boolean reduce(Boolean r1, Boolean r2) {
      if (r1 == null) {
        return r2;
      }
      if (r2 == null) {
        return r1;
      }
      return r1 || r2;
    }
  }
}
