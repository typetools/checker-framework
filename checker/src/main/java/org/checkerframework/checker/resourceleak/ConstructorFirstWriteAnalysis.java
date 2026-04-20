package org.checkerframework.checker.resourceleak;

import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.CatchTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TryTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreeScanner;
import java.util.List;
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
 * Utility class for the conservative check that determines whether a constructor assignment is the
 * first write to its field.
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
   * the constructor. This method is conservative: it returns {@code false} unless it can prove that
   * the write is the first.
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
   * @param assignment the assignment tree being analyzed, which is a statement in the body of
   *     {@code constructor}
   * @param targetField the non-final field assigned by {@code assignment}; its type is
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

    // (3): Single-pass conservative scan of the constructor body in source order.
    FirstWriteScanResult r =
        scanStatementsForFirstWrite(
            constructor.getBody().getStatements(), assignment, targetField, cmAtf);
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
      // Non-null inline initializer on the field declaration.
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
        if (InitializerAssignmentScanner.mayBeAssigned(initBlock, targetField, cmAtf)) {
          return true;
        }
      }
    }
    return false;
  }

  /** Result of scanning the constructor for the target assignment. */
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
   * Scans constructor-body statements in {@code stmts} in source order to determine whether {@code
   * targetAssignment} is definitely the first write to {@code targetField} in this constructor
   * fragment.
   *
   * <p>Field initializers and instance initializer blocks are checked separately by {@link
   * #isFirstWriteToFieldInConstructor(Tree, VariableElement, MethodTree,
   * RLCCalledMethodsAnnotatedTypeFactory)}.
   *
   * <p>This helper is conservative and does not model every statement form. Unsupported constructs
   * are documented inline below and conservatively return {@link
   * FirstWriteScanResult#REASSIGNMENT}.
   *
   * @param stmts statements to scan in source order
   * @param targetAssignment the assignment under test
   * @param targetField the field assigned by {@code targetAssignment}
   * @param cmAtf the factory used for side-effect reasoning
   * @return {@link FirstWriteScanResult#FIRST_ASSIGNMENT} if {@code targetAssignment} is reached
   *     before any disqualifying event; {@link FirstWriteScanResult#REASSIGNMENT} if an earlier
   *     write, disallowed call/allocation, or unsupported statement form prevents proving that;
   *     otherwise {@link FirstWriteScanResult#UNASSIGNED}
   */
  private static FirstWriteScanResult scanStatementsForFirstWrite(
      List<? extends StatementTree> stmts,
      Tree targetAssignment,
      VariableElement targetField,
      RLCCalledMethodsAnnotatedTypeFactory cmAtf) {
    for (StatementTree stmt : stmts) {
      if (stmt instanceof BlockTree blockTree) {
        // Nested blocks preserve source order, so scan them recursively.
        FirstWriteScanResult r =
            scanStatementsForFirstWrite(
                blockTree.getStatements(), targetAssignment, targetField, cmAtf);
        if (r != FirstWriteScanResult.UNASSIGNED) {
          return r;
        }
        continue;
      }

      if (stmt instanceof ExpressionStatementTree est) {
        FirstWriteScanResult res =
            ExpressionFirstWriteScanner.scanExpressionForFirstWrite(
                est.getExpression(), targetAssignment, targetField, cmAtf);
        if (res != FirstWriteScanResult.UNASSIGNED) {
          return res;
        }
        continue;
      }

      if (stmt instanceof VariableTree vt) {
        ExpressionTree init = vt.getInitializer();
        if (init != null) {
          FirstWriteScanResult res =
              ExpressionFirstWriteScanner.scanExpressionForFirstWrite(
                  init, targetAssignment, targetField, cmAtf);
          if (res != FirstWriteScanResult.UNASSIGNED) {
            return res;
          }
        }
        continue;
      }

      if (stmt instanceof TryTree tryTree) {

        // finally introduces ordering across try/catch that requires CFG reasoning
        if (tryTree.getFinallyBlock() != null) {
          return FirstWriteScanResult.REASSIGNMENT;
        }

        // try-with-resources evaluates resource initializers before the try body. Modeling those
        // effects would require extra handling, so reject.
        if (!tryTree.getResources().isEmpty()) {
          return FirstWriteScanResult.REASSIGNMENT;
        }

        // If any catch assigns the field, then initialization is path-dependent (try vs catch).
        // Without control-flow reasoning, conservatively reject.
        if (catchAssignsField(tryTree, targetField)) {
          return FirstWriteScanResult.REASSIGNMENT;
        }

        // Scan the try block body only (catch blocks are handled above).
        FirstWriteScanResult res =
            scanStatementsForFirstWrite(
                tryTree.getBlock().getStatements(), targetAssignment, targetField, cmAtf);
        if (res != FirstWriteScanResult.UNASSIGNED) {
          return res;
        }
        continue;
      }

      if (stmt instanceof IfTree ifTree) {
        // Scan the condition first and return any decisive result
        FirstWriteScanResult condRes =
            ExpressionFirstWriteScanner.scanExpressionForFirstWrite(
                ifTree.getCondition(), targetAssignment, targetField, cmAtf);
        if (condRes != FirstWriteScanResult.UNASSIGNED) {
          return condRes;
        }

        StatementTree thenStmt = ifTree.getThenStatement();
        StatementTree elseStmt = ifTree.getElseStatement();

        boolean targetInThen = containsTargetAssignment(thenStmt, targetAssignment);
        boolean targetInElse = containsTargetAssignment(elseStmt, targetAssignment);

        // If the target assignment is in one branch, only that branch is on the path to the
        // target assignment. Return the result after scanning that branch.
        if (targetInThen) {
          return scanStatementsForFirstWrite(
              List.of(thenStmt), targetAssignment, targetField, cmAtf);
        }
        if (targetInElse) {
          return scanStatementsForFirstWrite(
              List.of(elseStmt), targetAssignment, targetField, cmAtf);
        }

        // Otherwise, the target assignment is after the if statement, so either branch may execute
        // before the target assignment.
        FirstWriteScanResult thenRes =
            scanStatementsForFirstWrite(List.of(thenStmt), targetAssignment, targetField, cmAtf);

        // The else branch may not exist
        FirstWriteScanResult elseRes =
            elseStmt == null
                ? FirstWriteScanResult.UNASSIGNED
                : scanStatementsForFirstWrite(
                    List.of(elseStmt), targetAssignment, targetField, cmAtf);

        // If both branches are unassigned, then continue scanning after the if statement.
        if (thenRes == FirstWriteScanResult.UNASSIGNED
            && elseRes == FirstWriteScanResult.UNASSIGNED) {
          continue;
        }
        return FirstWriteScanResult.REASSIGNMENT;
      }

      // Any other statement kind requires control-flow-aware reasoning that this helper does not
      // attempt. Loops and switches can repeat or fall through before the target. Reject all of
      // them here.
      return FirstWriteScanResult.REASSIGNMENT;
    }

    return FirstWriteScanResult.UNASSIGNED;
  }

  /**
   * Returns true if any {@code catch} block of {@code tryTree} contains an assignment to {@code
   * targetField}.
   *
   * @param tryTree the try statement to inspect
   * @param targetField the field to check for assignments
   * @return true if any catch block assigns {@code targetField}
   */
  private static boolean catchAssignsField(
      TryTree tryTree, @FindDistinct VariableElement targetField) {
    // This scanner is used to check whether a catch block assigns the target field.
    TreeScanner<Boolean, Void> fieldAssignmentScanner =
        new BooleanShortCircuitScanner() {
          @Override
          public Boolean visitAssignment(AssignmentTree node, Void p) {
            Element lhsEl = TreeUtils.elementFromUse(node.getVariable());
            if (targetField.equals(lhsEl)) {
              return true;
            }
            return super.visitAssignment(node, p);
          }
        };
    for (CatchTree ct : tryTree.getCatches()) {
      boolean catchAssignsField = fieldAssignmentScanner.scan(ct.getBlock(), null);
      if (catchAssignsField) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns true if {@code tree} contains {@code targetAssignment}.
   *
   * @param tree the tree to scan
   * @param targetAssignment the assignment to look for
   * @return true if {@code targetAssignment} occurs within {@code tree}
   */
  private static boolean containsTargetAssignment(Tree tree, @FindDistinct Tree targetAssignment) {
    TreeScanner<Boolean, Void> targetScanner =
        new BooleanShortCircuitScanner() {
          @Override
          public Boolean visitAssignment(AssignmentTree node, Void p) {
            if (node == targetAssignment) {
              return true;
            }
            return super.visitAssignment(node, p);
          }
        };
    return targetScanner.scan(tree, null);
  }

  /**
   * Scanner that checks whether an instance initializer block assigns the target field.
   *
   * <p>It returns true if the initializer block either assigns the field directly or may do so
   * through a side-effecting method call or object creation.
   */
  private static final class InitializerAssignmentScanner extends BooleanShortCircuitScanner {

    /** The field being analyzed. */
    private final @InternedDistinct VariableElement targetField;

    /** The annotated type factory, used to determine whether a method has any side effects. */
    private final RLCCalledMethodsAnnotatedTypeFactory cmAtf;

    /**
     * Creates a scanner that checks whether an instance initializer block may assign {@code
     * targetField}.
     *
     * @param targetField the field being checked
     * @param cmAtf the type factory for side-effect reasoning
     */
    private InitializerAssignmentScanner(
        @FindDistinct VariableElement targetField, RLCCalledMethodsAnnotatedTypeFactory cmAtf) {
      this.targetField = targetField;
      this.cmAtf = cmAtf;
    }

    /**
     * Returns true if {@code initializerBlock} directly assigns {@code targetField} or may assign
     * it through a side-effecting call or allocation.
     *
     * @param initializerBlock the initializer block to scan
     * @param targetField the field under analyze
     * @param cmAtf the factory used for side-effect reasoning
     * @return true if {@code initializerBlock} may assign {@code targetField}
     */
    static boolean mayBeAssigned(
        BlockTree initializerBlock,
        @FindDistinct VariableElement targetField,
        RLCCalledMethodsAnnotatedTypeFactory cmAtf) {
      return new InitializerAssignmentScanner(targetField, cmAtf).scan(initializerBlock, null);
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
      // Any side-effecting method call in an initializer block could write to the field.
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
   * Scanner that scans an expression to determine whether a given assignment is definitely the
   * first write to its field before any earlier assignment or side-effecting call in that
   * expression. The entry point is {@link #scanExpressionForFirstWrite}.
   *
   * <p>This scanner is used only on expressions that {@link #scanStatementsForFirstWrite(List,
   * Tree, VariableElement, RLCCalledMethodsAnnotatedTypeFactory)} has already decided to scan. It
   * does not handle field initializers, static or instance initializer blocks, or statement-level
   * control-flow constructs.
   *
   * <p>If it is used on an unsupported fragment, the result is conservative only with respect to
   * the scanned expression; it does not account for surrounding control flow.
   */
  private static final class ExpressionFirstWriteScanner
      extends TreeScanner<FirstWriteScanResult, Void> {

    /** The assignment being analyzed. */
    private final @InternedDistinct Tree targetAssignment;

    /** The field assigned by {@code targetAssignment}. */
    private final VariableElement targetField;

    /** The annotated type factory, used to determine whether a method has any side effects. */
    private final RLCCalledMethodsAnnotatedTypeFactory cmAtf;

    /**
     * Creates a scanner that checks if {@code targetAssignment} is the first write to {@code
     * targetField} in the scanned expression. The scan stops as soon as a decisive result is
     * encountered.
     *
     * @param targetAssignment the assignment being analyzed
     * @param targetField the field written by that assignment
     * @param cmAtf the type factory for side-effect reasoning
     */
    private ExpressionFirstWriteScanner(
        @FindDistinct Tree targetAssignment,
        VariableElement targetField,
        RLCCalledMethodsAnnotatedTypeFactory cmAtf) {
      this.targetAssignment = targetAssignment;
      this.targetField = targetField;
      this.cmAtf = cmAtf;
    }

    /**
     * Scans {@code root} to determine whether {@code targetAssignment} is reached before any
     * disqualifying event within this tree fragment.
     *
     * <p>It returns {@link FirstWriteScanResult#FIRST_ASSIGNMENT} if the target assignment is
     * encountered before any earlier write to {@code targetField} or any potentially side-effecting
     * call/allocation. It returns {@link FirstWriteScanResult#REASSIGNMENT} if a disqualifying
     * event is encountered first. Otherwise, it returns {@link FirstWriteScanResult#UNASSIGNED} if
     * the target assignment does not appear in {@code root}.
     *
     * @param root the expression to scan
     * @param targetAssignment the target assignment
     * @param targetField the field assigned by {@code targetAssignment}
     * @param cmAtf the factory for side-effect reasoning
     * @return the scan result for {@code root}
     */
    static FirstWriteScanResult scanExpressionForFirstWrite(
        ExpressionTree root,
        @FindDistinct Tree targetAssignment,
        VariableElement targetField,
        RLCCalledMethodsAnnotatedTypeFactory cmAtf) {
      FirstWriteScanResult r =
          new ExpressionFirstWriteScanner(targetAssignment, targetField, cmAtf).scan(root, null);
      return r;
    }

    @Override
    public FirstWriteScanResult visitAssignment(AssignmentTree node, Void p) {
      Element lhsEl = TreeUtils.elementFromUse(node.getVariable());
      if (targetField.equals(lhsEl)) {
        // Found an assignment to the same field:
        //   - current assignment → first write → FIRST_ASSIGNMENT
        //   - earlier assignment → not first → REASSIGNMENT
        return node == targetAssignment
            ? FirstWriteScanResult.FIRST_ASSIGNMENT
            : FirstWriteScanResult.REASSIGNMENT;
      }
      return super.visitAssignment(node, p);
    }

    @Override
    public FirstWriteScanResult visitMethodInvocation(MethodInvocationTree node, Void p) {
      // Treat any method call before the target assignment as possibly assigning the field,
      // unless it is a side-effect-free method. An explicit super(...) call is also allowed,
      // because Java compiler adds a superclass-constructor call even when it is not written
      // explicitly.
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
    public FirstWriteScanResult scan(Tree tree, Void p) {
      if (tree == null) {
        return FirstWriteScanResult.UNASSIGNED;
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
      if (r1 != null && r1 != FirstWriteScanResult.UNASSIGNED) {
        return r1;
      }
      if (r2 == null) {
        return FirstWriteScanResult.UNASSIGNED;
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
   */
  private abstract static class BooleanShortCircuitScanner extends TreeScanner<Boolean, Void> {

    /** Do not instantiate. */
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
