package org.checkerframework.checker.collectionownership;

import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.ForLoopTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreeScanner;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.lang.model.element.Element;
import javax.lang.model.element.Name;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.resourceleak.ResourceLeakUtils;
import org.checkerframework.dataflow.cfg.ControlFlowGraph;
import org.checkerframework.dataflow.cfg.block.Block;
import org.checkerframework.dataflow.cfg.block.ConditionalBlock;
import org.checkerframework.dataflow.cfg.block.SingleSuccessorBlock;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.javacutil.TreeUtils;

/** Matches indexed `for` {@link DisposalLoop}'s that iterates over a resource collection. */
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
   * Returns the {@link DisposalLoop} if the `for` loop iterates over a resource collection and
   * follows a specific loop shape described in {@link #nameOfCollectionThatAllElementsAreCalledOn}.
   *
   * @param tree a `for` loop with exactly one loop variable
   * @return the matched disposal loop, or {@code null} if the loop does not match
   */
  @Nullable DisposalLoop match(ForLoopTree tree) {
    List<? extends StatementTree> loopBodyStatements =
        CollectionOwnershipUtils.asStatementList(tree.getStatement());
    if (loopBodyStatements == null) {
      return null;
    }
    StatementTree init = tree.getInitializer().get(0);
    ExpressionTree condition = TreeUtils.withoutParens(tree.getCondition());
    ExpressionStatementTree update = tree.getUpdate().get(0);
    if (!(condition instanceof BinaryTree binaryTreeCondition)) {
      return null;
    }
    Name identifierInHeader =
        nameOfCollectionThatAllElementsAreCalledOn(init, binaryTreeCondition, update);
    Name iterator = CollectionOwnershipUtils.getNameFromStatementTree(init);
    if (identifierInHeader == null || iterator == null) {
      return null;
    }
    ExpressionTree collectionElementTree =
        getLastElementAccessIfLoopValid(loopBodyStatements, identifierInHeader, iterator);
    if (collectionElementTree != null) {
      Block loopConditionBlock = CollectionOwnershipUtils.firstBlockForTree(cfg, condition);
      Block loopUpdateBlock =
          CollectionOwnershipUtils.firstBlockForTree(cfg, update.getExpression());
      Node nodeForCollectionElt =
          CollectionOwnershipUtils.anyNodeForTree(cfg, collectionElementTree);
      if (loopUpdateBlock == null || loopConditionBlock == null || nodeForCollectionElt == null) {
        return null;
      }
      Block conditionalBlock = ((SingleSuccessorBlock) loopConditionBlock).getSuccessor();
      Block loopBodyEntryBlock = ((ConditionalBlock) conditionalBlock).getThenSuccessor();
      return new DisposalLoop(
          CollectionOwnershipUtils.baseExpression(collectionElementTree),
          collectionElementTree,
          nodeForCollectionElt,
          CollectionOwnershipUtils.cfgAssociatedTreeFor(cfg, condition),
          (ConditionalBlock) conditionalBlock,
          loopBodyEntryBlock,
          loopUpdateBlock);
    }
    return null;
  }

  /**
   * Conservatively decides whether a loop iterates over all elements of some collection, using the
   * following rules:
   *
   * <ul>
   *   <li>only one loop variable
   *   <li>initialization must be of the form i = 0
   *   <li>condition must be of the form (i &lt; col.size())
   *   <li>update must be prefix or postfix {@code ++}
   * </ul>
   *
   * Returns:
   *
   * <ul>
   *   <li>null, if any of the above rules is violated
   *   <li>the name of the collection if the loop condition is of the form (i &lt; col.size())
   * </ul>
   *
   * @param init the initializer of the loop
   * @param condition the loop condition
   * @param update the loop update
   * @return the name of the collection that the loop iterates over all elements of, or null
   */
  Name nameOfCollectionThatAllElementsAreCalledOn(
      StatementTree init, BinaryTree condition, ExpressionStatementTree update) {
    Tree.Kind updateKind = update.getExpression().getKind();
    if (updateKind == Tree.Kind.PREFIX_INCREMENT || updateKind == Tree.Kind.POSTFIX_INCREMENT) {
      UnaryTree inc = (UnaryTree) update.getExpression();

      if (!(init instanceof VariableTree initVar)
          || !(inc.getExpression() instanceof IdentifierTree)) return null;

      if (!(initVar.getInitializer() instanceof LiteralTree)
          || !((LiteralTree) initVar.getInitializer()).getValue().equals(0)) {
        return null;
      }

      if (!(condition.getLeftOperand() instanceof IdentifierTree)) {
        return null;
      }

      Name initVarName = initVar.getName();
      if (initVarName != ((IdentifierTree) condition.getLeftOperand()).getName()) {
        return null;
      }
      if (initVarName != ((IdentifierTree) inc.getExpression()).getName()) {
        return null;
      }

      if ((condition.getRightOperand() instanceof MethodInvocationTree)
          && TreeUtils.isSizeAccess(condition.getRightOperand())) {
        ExpressionTree methodSelect =
            ((MethodInvocationTree) condition.getRightOperand()).getMethodSelect();
        if (methodSelect instanceof MemberSelectTree mst) {
          Element elt = TreeUtils.elementFromTree(mst.getExpression());
          if (ResourceLeakUtils.isCollection(elt, coAtf)) {
            return CollectionOwnershipUtils.getNameFromExpressionTree(mst.getExpression());
          }
        }
      }
    }
    return null;
  }

  /**
   * Check that the loop does not contain any writes to the loop iterator variable or to the
   * collection variable itself. Extract the collection access tree ({@code arr[i]} or {@code
   * collection.get(i)} where {@code i} is the iterator variable and {@code collection/arr} is
   * consistent with the loop header) and return the last encountered such tree.
   *
   * @param statements list of statements of the loop body
   * @param identifierInHeader collection name if loop condition is {@code i < collection.size()} or
   *     {@code i < arr.length} and {@code n} if loop condition is {@code i < n}
   * @param iterator the name of the loop iterator variable
   * @return {@code null} if the loop body writes to the iterator or collection variable; otherwise
   *     the last collection element access tree consistent with the loop header, if one exists
   */
  private @Nullable ExpressionTree getLastElementAccessIfLoopValid(
      List<? extends StatementTree> statements, Name identifierInHeader, Name iterator) {
    AtomicBoolean blockIsIllegal = new AtomicBoolean(false);
    final ExpressionTree[] collectionElementTree = {null};

    TreeScanner<Void, Void> scanner =
        new TreeScanner<Void, Void>() {
          @Override
          public Void visitUnary(UnaryTree tree, Void p) {
            switch (tree.getKind()) {
              case PREFIX_DECREMENT, POSTFIX_DECREMENT, PREFIX_INCREMENT, POSTFIX_INCREMENT -> {
                if (CollectionOwnershipUtils.getNameFromExpressionTree(tree.getExpression())
                    == iterator) {
                  blockIsIllegal.set(true);
                }
              }
              default -> {}
            }
            return super.visitUnary(tree, p);
          }

          @Override
          public Void visitCompoundAssignment(CompoundAssignmentTree tree, Void p) {
            if (CollectionOwnershipUtils.getNameFromExpressionTree(tree.getVariable())
                == iterator) {
              blockIsIllegal.set(true);
            }
            return super.visitCompoundAssignment(tree, p);
          }

          @Override
          public Void visitAssignment(AssignmentTree tree, Void p) {
            Name assignedVariable =
                CollectionOwnershipUtils.getNameFromExpressionTree(tree.getVariable());
            if (assignedVariable == iterator || assignedVariable == identifierInHeader) {
              blockIsIllegal.set(true);
            }

            return super.visitAssignment(tree, p);
          }

          @Override
          public Void visitMethodInvocation(MethodInvocationTree mit, Void p) {
            if (isIthCollectionElement(mit, iterator)
                && identifierInHeader == CollectionOwnershipUtils.getNameFromExpressionTree(mit)
                && identifierInHeader != null) {
              collectionElementTree[0] = mit;
            }
            return super.visitMethodInvocation(mit, p);
          }
        };

    for (StatementTree stmt : statements) {
      scanner.scan(stmt, null);
    }
    if (!blockIsIllegal.get() && collectionElementTree[0] != null) {
      return collectionElementTree[0];
    }
    return null;
  }

  /**
   * Returns true if the given tree is of the form collection.get(i), where i is the given index
   * name.
   *
   * @param tree the tree to check
   * @param index the index variable name
   * @return true if the given tree is of the form collection.get(index)
   */
  private boolean isIthCollectionElement(Tree tree, Name index) {
    if (tree == null || index == null) {
      return false;
    }
    if (tree instanceof MethodInvocationTree mit
        && index
            == CollectionOwnershipUtils.getNameFromExpressionTree(
                TreeUtils.getIdxForGetCall(tree))) {
      ExpressionTree methodSelect = mit.getMethodSelect();
      if (methodSelect instanceof MemberSelectTree mst) {
        Element receiverElt = TreeUtils.elementFromTree(mst.getExpression());
        return ResourceLeakUtils.isCollection(receiverElt, coAtf);
      }
    }
    return false;
  }
}
