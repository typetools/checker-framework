package org.checkerframework.checker.resourceleak;

import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.CatchTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ExpressionTree;
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
import java.util.concurrent.atomic.AtomicBoolean;
import javax.lang.model.element.Element;
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

  /**
   * The entry point is {@link #isFirstWriteToFieldInConstructor(Tree, VariableElement, MethodTree,
   * RLCCalledMethodsAnnotatedTypeFactory)}.
   */
  private ConstructorFirstWriteAnalysis() {}

  /**
   * Returns true if the given assignment is the first write to {@code targetField} on its path in
   * the constructor. This method is conservative: it returns {@code false} unless it can prove that
   * the write is the first. This check runs only for non-final fields because the Java compiler
   * already forbids reassignment of final fields.
   *
   * <p>The result is {@code true} only if all the following hold:
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
   * @param targetField the field assigned by {@code assignment}; its type is non-primitive
   * @param constructor the constructor where the assignment appears
   * @param cmAtf the factory used for side-effect reasoning and tree-path lookup
   * @return true if this assignment is the first write during construction
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

    for (Tree member : classTree.getMembers()) {
      // (1) Disallow non-null inline initializer on the same field declaration.
      if (member instanceof VariableTree decl) {
        VariableElement declElement = TreeUtils.elementFromDeclaration(decl);
        if (targetField == declElement
            && decl.getInitializer() != null
            && decl.getInitializer().getKind() != Tree.Kind.NULL_LITERAL) {
          return false;
        }
        continue;
      }
      // (2) Disallow assignment in any instance initializer block.
      if (member instanceof BlockTree initBlock) {
        if (initBlock.isStatic()) {
          continue;
        }
        // The variables accessed from within the anonymous class need to be effectively final, so
        // AtomicBoolean is used here.
        AtomicBoolean isInitialized = new AtomicBoolean(false);
        initBlock.accept(
            new TreeScanner<Void, Void>() {
              @Override
              public Void scan(Tree tree, Void unused) {
                // Stop descending once an earlier write or other disqualifying side effect is
                // found.
                if (isInitialized.get() || tree == null) {
                  return null;
                }
                return super.scan(tree, unused);
              }

              @Override
              public Void scan(Iterable<? extends Tree> trees, Void unused) {
                // Stop visiting sibling nodes once a disqualifying node has been found.
                if (isInitialized.get() || trees == null) {
                  return null;
                }
                for (Tree tree : trees) {
                  if (isInitialized.get()) {
                    return null;
                  }
                  scan(tree, unused);
                }
                return null;
              }

              @Override
              public Void visitAssignment(AssignmentTree assignmentTree, Void unused) {
                ExpressionTree lhs = assignmentTree.getVariable();
                Element lhsElement = TreeUtils.elementFromTree(lhs);
                if (targetField == lhsElement) {
                  isInitialized.set(true);
                  return null;
                }
                return super.visitAssignment(assignmentTree, unused);
              }

              @Override
              public Void visitMethodInvocation(MethodInvocationTree node, Void unused) {
                // Any side-effecting method call in an initializer block could write to the field.
                if (!cmAtf.isSideEffectFree(TreeUtils.elementFromUse(node))) {
                  isInitialized.set(true);
                  return null;
                }
                return super.visitMethodInvocation(node, unused);
              }

              @Override
              public Void visitNewClass(NewClassTree node, Void unused) {
                if (!cmAtf.isSideEffectFree(TreeUtils.elementFromUse(node))) {
                  isInitialized.set(true);
                  return null;
                }
                return super.visitNewClass(node, unused);
              }
            },
            null);
        if (isInitialized.get()) {
          return false;
        }
      }
    }

    // (3): Single-pass conservative scan of the constructor body in source order.
    FirstWriteScanResult r =
        scanForFirstWrite(constructor.getBody().getStatements(), assignment, targetField, cmAtf);
    return r == FirstWriteScanResult.FIRST_ASSIGNMENT;
  }

  /** Result of scanning the constructor for the target assignment under the conservative rules. */
  private enum FirstWriteScanResult {
    /** The target assignment is definitely the first assignment in the scanned region. */
    FIRST_ASSIGNMENT,
    /**
     * Disqualified by an earlier write, disallowed call/allocation, or unsupported statement form.
     */
    REASSIGNMENT,
    /** The target assignment does not occur in the scanned region. */
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
   *     otherwise {@link FirstWriteScanResult#UNASSIGNED} if the target assignment does not occur
   *     in the scanned region
   */
  private static FirstWriteScanResult scanForFirstWrite(
      List<? extends StatementTree> stmts,
      Tree targetAssignment,
      VariableElement targetField,
      RLCCalledMethodsAnnotatedTypeFactory cmAtf) {
    for (StatementTree stmt : stmts) {
      if (stmt instanceof BlockTree blockTree) {
        // Nested blocks preserve source order, so scan them recursively.
        FirstWriteScanResult r =
            scanForFirstWrite(blockTree.getStatements(), targetAssignment, targetField, cmAtf);
        if (r != FirstWriteScanResult.UNASSIGNED) {
          return r;
        }
        continue;
      }

      if (stmt instanceof ExpressionStatementTree est) {
        // Expression statements execute here in source order, so scan the expression subtree.
        FirstWriteScanResult res =
            ConstructorFirstWriteScanner.isFirstWrite(
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
              ConstructorFirstWriteScanner.isFirstWrite(init, targetAssignment, targetField, cmAtf);
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
            scanForFirstWrite(
                tryTree.getBlock().getStatements(), targetAssignment, targetField, cmAtf);
        if (res != FirstWriteScanResult.UNASSIGNED) {
          return res;
        }
        continue;
      }

      // Any other statement kind requires control-flow-aware reasoning that this helper does not
      // attempt. Loops and switches can repeat or fall through before the target. An `if` could
      // be handled more precisely by a more path-sensitive implementation, but this AST-only
      // helper does not distinguish which branch executes, so it conservatively rejects all of
      // them here.
      return FirstWriteScanResult.REASSIGNMENT;
    }

    return FirstWriteScanResult.UNASSIGNED;
  }

  /**
   * Returns true if any {@code catch} block of {@code tryTree} contains an assignment to {@code
   * targetField}.
   *
   * <p>This is used to conservatively reject {@code try/catch} regions where initialization becomes
   * path-dependent (a write may occur in {@code try} on the normal path or in {@code catch} on an
   * exceptional path).
   *
   * @param tryTree the try statement to inspect
   * @param targetField the field to check for assignments
   * @return true if any catch block assigns {@code targetField}
   */
  private static boolean catchAssignsField(TryTree tryTree, VariableElement targetField) {
    for (CatchTree ct : tryTree.getCatches()) {
      AtomicBoolean assigns = new AtomicBoolean(false);
      ct.getBlock()
          .accept(
              new TreeScanner<Void, Void>() {
                @Override
                public Void visitAssignment(AssignmentTree node, Void p) {
                  Element lhsEl = TreeUtils.elementFromUse(node.getVariable());
                  if (targetField.equals(lhsEl)) {
                    assigns.set(true);
                    return null;
                  }
                  return super.visitAssignment(node, p);
                }
              },
              null);
      if (assigns.get()) {
        return true;
      }
    }
    return false;
  }

  /**
   * Visitor that scans an expression subtree to determine whether a given constructor assignment is
   * definitely the first write to its field before any earlier assignment or side-effecting call in
   * that subtree. The entry point is {@link #isFirstWrite}.
   *
   * <p>This visitor is used only on expression subtrees from constructor-body expression statements
   * and local-variable initializers that {@link #scanForFirstWrite(List, Tree, VariableElement,
   * RLCCalledMethodsAnnotatedTypeFactory)} has already decided to scan. It does not handle field
   * initializers, static or instance initializer blocks, or statement-level control-flow
   * constructs.
   *
   * <p>If it is used on an unsupported fragment, the result is conservative only with respect to
   * the scanned expression subtree; it does not account for surrounding control flow.
   */
  private static final class ConstructorFirstWriteScanner
      extends TreeScanner<FirstWriteScanResult, Void> {

    /** The assignment under test within the constructor. */
    private final @InternedDistinct Tree targetAssignment;

    /** The field assigned by {@code targetAssignment}. */
    private final VariableElement targetField;

    /** The annotated type factory, used to determine whether a method has any side effects. */
    private final RLCCalledMethodsAnnotatedTypeFactory cmAtf;

    /**
     * Set once scanning reaches {@link FirstWriteScanResult#FIRST_ASSIGNMENT} or {@link
     * FirstWriteScanResult#REASSIGNMENT}, to short-circuit further traversal.
     */
    private FirstWriteScanResult scanResult = FirstWriteScanResult.UNASSIGNED;

    /**
     * Creates a scanner that checks if {@code assignment} is the first write to {@code targetField}
     * within the current constructor. The scan stops as soon as a decisive result (true/false) is
     * encountered.
     *
     * @param targetAssignment the assignment being analyzed
     * @param targetField the field written by that assignment
     * @param cmAtf the type factory for side-effect reasoning
     */
    private ConstructorFirstWriteScanner(
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
     * <p>This method reasons only about the syntax tree rooted at {@code root}; it does not build a
     * control-flow graph, perform path-sensitive reasoning, or inspect callees. It returns {@link
     * FirstWriteScanResult#FIRST_ASSIGNMENT} if the target assignment is encountered before any
     * earlier write to {@code targetField} or any potentially side-effecting call/allocation
     * (except {@code super(...)}). It returns {@link FirstWriteScanResult#REASSIGNMENT} if a
     * disqualifying event is encountered first. Otherwise, it returns {@link
     * FirstWriteScanResult#UNASSIGNED} if the target assignment does not appear in {@code root}.
     *
     * @param root the statement to scan
     * @param targetAssignment the target assignment
     * @param targetField the field assigned by {@code targetAssignment}
     * @param cmAtf the factory for side-effect reasoning
     * @return the scan result for {@code root}
     */
    static FirstWriteScanResult isFirstWrite(
        ExpressionTree root,
        @FindDistinct Tree targetAssignment,
        VariableElement targetField,
        RLCCalledMethodsAnnotatedTypeFactory cmAtf) {
      FirstWriteScanResult r =
          new ConstructorFirstWriteScanner(targetAssignment, targetField, cmAtf).scan(root, null);
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
      // unless it is a side-effect-free method or a super(...) constructor call.
      if (cmAtf.isSideEffectFree(TreeUtils.elementFromUse(node))
          || TreeUtils.isSuperConstructorCall(node)) {
        return super.visitMethodInvocation(node, p);
      }
      return FirstWriteScanResult.REASSIGNMENT;
    }

    @Override
    public FirstWriteScanResult visitNewClass(NewClassTree node, Void p) {
      // An object creation with side effects can modify constructor fields (e.g., via Helper(this),
      // where `this` can be modified in Helper's constructor).
      if (cmAtf.isSideEffectFree(TreeUtils.elementFromUse(node))) {
        return super.visitNewClass(node, p);
      }
      return FirstWriteScanResult.REASSIGNMENT;
    }

    @Override
    public FirstWriteScanResult scan(Tree tree, Void p) {
      if (scanResult != FirstWriteScanResult.UNASSIGNED || tree == null) {
        return scanResult;
      }
      FirstWriteScanResult result = super.scan(tree, p);
      if (result != null && result != FirstWriteScanResult.UNASSIGNED) {
        scanResult = result;
      }
      return scanResult;
    }

    @Override
    public FirstWriteScanResult scan(Iterable<? extends Tree> trees, Void p) {
      if (scanResult != FirstWriteScanResult.UNASSIGNED || trees == null) {
        return scanResult;
      }
      for (Tree tree : trees) {
        FirstWriteScanResult result = scan(tree, p);
        if (result != FirstWriteScanResult.UNASSIGNED) {
          return result;
        }
      }
      return scanResult;
    }
  }
}
