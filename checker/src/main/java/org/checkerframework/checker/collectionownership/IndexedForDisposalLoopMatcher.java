package org.checkerframework.checker.collectionownership;

import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.ForLoopTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreeScanner;
import javax.lang.model.element.Name;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.cfg.ControlFlowGraph;
import org.checkerframework.dataflow.cfg.block.Block;
import org.checkerframework.dataflow.cfg.block.ConditionalBlock;
import org.checkerframework.dataflow.cfg.block.SingleSuccessorBlock;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.javacutil.TreeUtils;

/** Matches indexed `for` {@link DisposalLoopInfo}s that iterate over a resource collection. */
final class IndexedForDisposalLoopMatcher {

  /** The CO type factory used for collection-ownership queries. */
  private final CollectionOwnershipAnnotatedTypeFactory coAtf;

  /** The CFG of the method currently being scanned. */
  private final ControlFlowGraph cfg;

  /**
   * Creates a matcher for indexed `for` disposal loops.
   *
   * @param coAtf the CO type factory
   * @param cfg the CFG being scanned
   */
  IndexedForDisposalLoopMatcher(
      CollectionOwnershipAnnotatedTypeFactory coAtf, ControlFlowGraph cfg) {
    this.coAtf = coAtf;
    this.cfg = cfg;
  }

  /**
   * Returns the {@link DisposalLoopInfo} if the {@code for} loop {@code tree} iterates over a
   * resource collection satisfying the following rules:
   *
   * <ul>
   *   <li>exactly one loop initializer statement and one update expression
   *   <li>initialization must be of the form {@code int i = 0}
   *   <li>condition must be of the form {@code i < collection.size()}
   *   <li>update must be prefix or postfix {@code ++}
   *   <li>no overwrite of the index variable or collection variable in the loop body
   *   <li>at least one access of the iterated collection element consistent with the loop header
   *       (e.g., {@code collection.get(i)}) in the loop body
   * </ul>
   *
   * <p>For example:
   *
   * <pre>{@code
   * for (int i = 0; i < collection.size(); i++) {
   *   Resource resource = collection.get(i);
   *   resource.close();
   * }
   * }</pre>
   *
   * @param tree the `for` loop to inspect
   * @return the matched disposal loop info, or {@code null} if the loop does not match
   */
  @Nullable DisposalLoopInfo match(ForLoopTree tree) {
    // Reject the loop if it doesn't have exactly one initializer and one update.
    if (tree.getInitializer().size() != 1 || tree.getUpdate().size() != 1) {
      return null;
    }

    StatementTree initializer = tree.getInitializer().get(0);
    ExpressionStatementTree update = tree.getUpdate().get(0);
    ExpressionTree rawCondition = tree.getCondition();
    if (rawCondition == null) {
      return null;
    }
    ExpressionTree condition = TreeUtils.withoutParens(rawCondition);
    if (!(condition instanceof BinaryTree binaryTreeCondition)) {
      return null;
    }

    Name indexVariableName = indexVariableNameIfCanonicalIndexLoop(initializer, update);
    if (indexVariableName == null) {
      return null;
    }

    Name collectionName =
        resourceCollectionNameIfConditionMatches(binaryTreeCondition, indexVariableName);
    if (collectionName == null) {
      return null;
    }

    // Validate the loop body and recover the last `collection.get(i)` access that represents the
    // iterated element for this loop.
    ExpressionTree collectionElementTree =
        new LoopBodyScanner(collectionName, indexVariableName).scanLoopBody(tree.getStatement());
    if (collectionElementTree == null) {
      return null;
    }

    ExpressionTree collectionExpression =
        CollectionOwnershipUtils.baseExpression(collectionElementTree);
    if (collectionExpression == null) {
      return null;
    }

    // After the tree match succeeds, recover the CFG blocks corresponding to the loop condition,
    // body entry, and loop update.
    Block loopConditionBlock = CollectionOwnershipUtils.firstBlockForTree(cfg, condition);
    Block loopUpdateBlock = CollectionOwnershipUtils.firstBlockForTree(cfg, update.getExpression());
    Node iteratedElementNode = CollectionOwnershipUtils.anyNodeForTree(cfg, collectionElementTree);
    if (loopConditionBlock == null || loopUpdateBlock == null || iteratedElementNode == null) {
      return null;
    }
    if (!(loopConditionBlock instanceof SingleSuccessorBlock singleSuccessorLoopConditionBlock)) {
      return null;
    }
    Block conditionalBlockCandidate = singleSuccessorLoopConditionBlock.getSuccessor();
    if (!(conditionalBlockCandidate instanceof ConditionalBlock conditionalBlock)) {
      return null;
    }
    // The then-successor is the body-entry block for the matching indexed loop.
    Block loopBodyEntryBlock = conditionalBlock.getThenSuccessor();

    return new DisposalLoopInfo(
        collectionExpression,
        collectionElementTree,
        iteratedElementNode,
        CollectionOwnershipUtils.cfgAssociatedTreeFor(cfg, condition),
        conditionalBlock,
        loopBodyEntryBlock,
        loopUpdateBlock);
  }

  /**
   * Returns true if the given tree is of the form {@code collection.get(i)}, where {@code i} is the
   * given index variable name.
   *
   * @param tree the tree to check
   * @param indexVariableName the index variable name
   * @return true if the tree is of the form {@code collection.get(i)}
   */
  private boolean isIthCollectionElement(Tree tree, Name indexVariableName) {
    if (tree instanceof MethodInvocationTree methodInvocationTree
        && indexVariableName
            == CollectionOwnershipUtils.getNameFromExpressionTree(
                TreeUtils.getIdxForGetCall(tree))) {
      ExpressionTree methodSelect = methodInvocationTree.getMethodSelect();
      if (methodSelect instanceof MemberSelectTree memberSelectTree) {
        return coAtf.isResourceCollection(memberSelectTree.getExpression());
      }
    }
    return false;
  }

  /**
   * Returns the index variable name if the initializer and update form a canonical zero-based index
   * loop.
   *
   * <p>This checks the {@code int i = 0} and {@code i++}/{@code ++i} parts of the loop header.
   *
   * @param initializer the loop initializer
   * @param update the loop update
   * @return the index variable name, or {@code null} if the initializer/update do not match
   */
  private @Nullable Name indexVariableNameIfCanonicalIndexLoop(
      StatementTree initializer, ExpressionStatementTree update) {
    Tree.Kind updateKind = update.getExpression().getKind();
    if (updateKind != Tree.Kind.PREFIX_INCREMENT && updateKind != Tree.Kind.POSTFIX_INCREMENT) {
      return null;
    }

    if (!(initializer instanceof VariableTree initVariable)) {
      return null;
    }
    if (!(update.getExpression() instanceof UnaryTree incrementExpression)) {
      return null;
    }
    if (!(incrementExpression.getExpression() instanceof IdentifierTree updateIdentifier)) {
      return null;
    }

    ExpressionTree initializerValue = initVariable.getInitializer();
    if (!(initializerValue instanceof LiteralTree literalTree)
        || !literalTree.getValue().equals(0)) {
      return null;
    }

    Name indexVariableName = initVariable.getName();
    return indexVariableName == updateIdentifier.getName() ? indexVariableName : null;
  }

  /**
   * Returns the collection name if the loop condition is of the form {@code i < collection.size()}
   * for the given index variable and the receiver {@code collection} is a resource collection.
   *
   * @param condition the loop condition
   * @param indexVariableName the validated loop index variable
   * @return the collection name, or {@code null} if the condition does not match
   */
  private @Nullable Name resourceCollectionNameIfConditionMatches(
      BinaryTree condition, Name indexVariableName) {
    if (condition.getKind() != Tree.Kind.LESS_THAN) {
      return null;
    }
    if (!(condition.getLeftOperand() instanceof IdentifierTree conditionIdentifier)) {
      return null;
    }
    if (conditionIdentifier.getName() != indexVariableName) {
      return null;
    }
    if (!(condition.getRightOperand() instanceof MethodInvocationTree)
        || !TreeUtils.isSizeAccess(condition.getRightOperand())) {
      return null;
    }

    ExpressionTree methodSelect =
        ((MethodInvocationTree) condition.getRightOperand()).getMethodSelect();
    if (!(methodSelect instanceof MemberSelectTree memberSelectTree)) {
      return null;
    }
    if (!coAtf.isResourceCollection(memberSelectTree.getExpression())) {
      return null;
    }
    return CollectionOwnershipUtils.getNameFromExpressionTree(memberSelectTree.getExpression());
  }

  /**
   * Scans an indexed {@code for} loop body, rejecting writes that invalidate the simple
   * indexed-loop model and remembers the last matching {@code collection.get(i)} access.
   */
  private final class LoopBodyScanner extends TreeScanner<Void, Void> {

    /** The collection named in the loop header. */
    private final Name collectionName;

    /** The index variable named in the loop header. */
    private final Name indexVariableName;

    /** Whether the loop body mutates the collection or index variable. */
    private boolean bodyIsIllegal = false;

    /** The last matching {@code collection.get(i)} access found in the body. */
    private @Nullable ExpressionTree lastCollectionElementAccess = null;

    /**
     * Creates the {@link LoopBodyScanner}.
     *
     * @param collectionName the collection name from the loop header
     * @param indexVariableName the index variable name from the loop header
     */
    private LoopBodyScanner(Name collectionName, Name indexVariableName) {
      this.collectionName = collectionName;
      this.indexVariableName = indexVariableName;
    }

    /**
     * Scans the loop body once and returns the last matching element access if the body remains
     * compatible with indexed disposal-loop matching.
     *
     * @param loopBody the loop body to scan
     * @return the last matching {@code collection.get(i)} access, or {@code null} if the body
     *     writes to the collection or index variable, or if no matching element access is found
     */
    private @Nullable ExpressionTree scanLoopBody(StatementTree loopBody) {
      super.scan(loopBody, null);
      return bodyIsIllegal ? null : lastCollectionElementAccess;
    }

    @Override
    public Void visitUnary(UnaryTree tree, Void p) {
      switch (tree.getKind()) {
        case PREFIX_DECREMENT, POSTFIX_DECREMENT, PREFIX_INCREMENT, POSTFIX_INCREMENT -> {
          if (CollectionOwnershipUtils.getNameFromExpressionTree(tree.getExpression())
              == indexVariableName) {
            bodyIsIllegal = true;
            return null;
          }
        }
        default -> {}
      }
      return super.visitUnary(tree, p);
    }

    @Override
    public Void visitCompoundAssignment(CompoundAssignmentTree tree, Void p) {
      if (CollectionOwnershipUtils.getNameFromExpressionTree(tree.getVariable())
          == indexVariableName) {
        bodyIsIllegal = true;
        return null;
      }
      return super.visitCompoundAssignment(tree, p);
    }

    @Override
    public Void visitAssignment(AssignmentTree tree, Void p) {
      Name assignedVariable =
          CollectionOwnershipUtils.getNameFromExpressionTree(tree.getVariable());
      // Invalidate if writes to the collection or the loop index variable.
      if (assignedVariable == indexVariableName || assignedVariable == collectionName) {
        bodyIsIllegal = true;
        return null;
      }
      return super.visitAssignment(tree, p);
    }

    @Override
    public Void visitMethodInvocation(MethodInvocationTree tree, Void p) {
      if (isIthCollectionElement(tree, indexVariableName)
          && collectionName == CollectionOwnershipUtils.getNameFromExpressionTree(tree)) {
        // The last matching access represents the iterated element for this loop.
        lastCollectionElementAccess = tree;
      }
      return super.visitMethodInvocation(tree, p);
    }

    @Override
    public Void visitLambdaExpression(LambdaExpressionTree tree, Void p) {
      // A lambda body is not executed as part of the enclosing loop body.
      return null;
    }

    @Override
    public Void visitClass(ClassTree tree, Void p) {
      // Skip local and anonymous class bodies for the same reason as lambdas.
      return null;
    }

    @Override
    public @Nullable Void scan(@Nullable Tree tree, Void p) {
      // Short-circuit the scanner if the collection/index variable is mutated.
      if (bodyIsIllegal) {
        return null;
      }
      return super.scan(tree, p);
    }

    @Override
    public @Nullable Void scan(@Nullable Iterable<? extends Tree> trees, Void p) {
      if (bodyIsIllegal) {
        return null;
      }
      return super.scan(trees, p);
    }
  }
}
