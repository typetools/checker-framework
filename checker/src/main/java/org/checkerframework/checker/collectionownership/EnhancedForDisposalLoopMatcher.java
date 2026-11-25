package org.checkerframework.checker.collectionownership;

import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreeScanner;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.lang.model.element.Element;
import javax.lang.model.element.Name;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.interning.qual.FindDistinct;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.cfg.ControlFlowGraph;
import org.checkerframework.dataflow.cfg.block.Block;
import org.checkerframework.dataflow.cfg.block.ConditionalBlock;
import org.checkerframework.dataflow.cfg.block.SingleSuccessorBlock;
import org.checkerframework.dataflow.cfg.node.AssignmentNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.javacutil.BugInCF;
import org.plumelib.util.IPair;

/** Matches enhanced-`for` {@link DisposalLoopInfo} from CFG. */
final class EnhancedForDisposalLoopMatcher {

  /** The CO type factory used for collection-ownership queries. */
  private final CollectionOwnershipAnnotatedTypeFactory coAtf;

  /** The CFG of the method currently being scanned. */
  private final ControlFlowGraph cfg;

  /**
   * Creates a matcher for enhanced-`for` disposal loops.
   *
   * @param coAtf the CO type factory
   * @param cfg the CFG being scanned
   */
  EnhancedForDisposalLoopMatcher(
      CollectionOwnershipAnnotatedTypeFactory coAtf, ControlFlowGraph cfg) {
    this.coAtf = coAtf;
    this.cfg = cfg;
  }

  /**
   * Returns a {@link DisposalLoopInfo} for each CFG copy of the enhanced-for-loop, if the loop
   * iterates over a resource collection and its body neither assigns to the variable that holds the
   * collection nor inserts into or replaces elements of the collection.
   *
   * <p>A loop can have several CFG copies, because the CFG copies a {@code finally} block for each
   * way of leaving its {@code try} block. For example, the CFG has one copy of this loop that the
   * {@code return} reaches, and another that normal completion of the {@code try} block reaches:
   *
   * <pre>{@code
   * try {
   *   if (b) {
   *     return;
   *   }
   * } finally {
   *   for (Resource r : list) {
   *     r.close();
   *   }
   * }
   * }</pre>
   *
   * @param tree the enhanced-for-loop to inspect
   * @return a disposal loop info for each CFG copy of the loop, or an empty set if the loop does
   *     not match
   */
  Set<DisposalLoopInfo> match(EnhancedForLoopTree tree) {
    if (!coAtf.isResourceCollection(tree.getExpression())) {
      return Set.of();
    }
    // If the body reassigns the collection variable, the loop still iterates over the original
    // collection, but after the loop the variable refers to a different collection.
    Element collectionElement =
        CollectionOwnershipUtils.getElementFromExpressionTree(tree.getExpression());
    if (collectionElement != null && assignsTo(tree.getStatement(), collectionElement)) {
      return Set.of();
    }
    // If the body inserts into or replaces elements of the collection, the iterator might not visit
    // the new elements, as for CopyOnWriteArrayList.
    if (mutatesIteratedCollection(tree.getStatement(), tree.getExpression())) {
      return Set.of();
    }
    return resolveEnhancedForLoop(tree);
  }

  /**
   * Returns true if {@code body} contains an assignment to the given variable or field.
   *
   * @param body a loop body
   * @param variable a variable or field
   * @return true if {@code body} assigns to {@code variable}
   */
  private static boolean assignsTo(StatementTree body, Element variable) {
    @Nullable Boolean result =
        new TreeScanner<Boolean, Void>() {
          @Override
          public Boolean visitAssignment(AssignmentTree tree, Void p) {
            if (variable.equals(
                CollectionOwnershipUtils.getElementFromExpressionTree(tree.getVariable()))) {
              return true;
            }
            return super.visitAssignment(tree, p);
          }

          @Override
          public Boolean reduce(@Nullable Boolean r1, @Nullable Boolean r2) {
            return Boolean.TRUE.equals(r1) || Boolean.TRUE.equals(r2);
          }
        }.scan(body, null);
    return Boolean.TRUE.equals(result);
  }

  /**
   * Returns true if {@code body} might insert into or replace elements of {@code collection}: it
   * calls a {@code @CreatesCollectionObligation} method or a replacing method on the collection,
   * or, if the collection is an owning collection field of {@code this}, a method whose
   * {@code @CreatesMustCallFor} target is {@code this}.
   *
   * @param body a loop body
   * @param collection the collection that the loop iterates over
   * @return true if {@code body} might insert into or replace elements of {@code collection}
   */
  private boolean mutatesIteratedCollection(StatementTree body, ExpressionTree collection) {
    Name collectionName = CollectionOwnershipUtils.getNameFromExpressionTree(collection);
    boolean isFieldOfThis =
        CollectionOwnershipUtils.isOwningCollectionFieldOfThis(collection, coAtf);
    @Nullable Boolean result =
        new TreeScanner<Boolean, Void>() {
          @Override
          public Boolean visitMethodInvocation(MethodInvocationTree tree, Void p) {
            if (collectionName != null
                && CollectionOwnershipUtils.insertsIntoOrReplacesElementsOf(
                    tree, collectionName, coAtf)) {
              return true;
            }
            if (isFieldOfThis
                && CollectionOwnershipUtils.createsMustCallForThis(tree, cfg, coAtf)) {
              return true;
            }
            return super.visitMethodInvocation(tree, p);
          }

          @Override
          public Boolean reduce(@Nullable Boolean r1, @Nullable Boolean r2) {
            return Boolean.TRUE.equals(r1) || Boolean.TRUE.equals(r2);
          }
        }.scan(body, null);
    return Boolean.TRUE.equals(result);
  }

  /**
   * Searches the method CFG for every copy of {@code tree} and resolves each to a {@link
   * DisposalLoopInfo}. See {@link #match}.
   *
   * <p>This performs a CFG traversal, looking for the desugared iterator {@code next()} node tagged
   * with the target enhanced-for-loop. (Only the {@code next()} node is tagged; the {@code
   * hasNext()} node is not.)
   *
   * @param tree the enhanced-for-loop to resolve
   * @return the CFG-resolved loop infos, one per CFG copy that can be resolved
   */
  private Set<DisposalLoopInfo> resolveEnhancedForLoop(EnhancedForLoopTree tree) {
    Block entryBlock = cfg.getEntryBlock();
    Set<Block> visitedBlocks = new HashSet<>();
    Deque<Block> worklist = new ArrayDeque<>();
    worklist.add(entryBlock);
    visitedBlocks.add(entryBlock);
    Set<DisposalLoopInfo> resolvedLoops = new LinkedHashSet<>();

    while (!worklist.isEmpty()) {
      Block currentBlock = worklist.removeFirst();
      for (Node node : currentBlock.getNodes()) {
        if (node instanceof MethodInvocationNode methodInvocationNode
            && isTargetEnhancedForInvocation(methodInvocationNode, tree)) {
          DisposalLoopInfo resolvedLoop = resolveEnhancedForLoop(methodInvocationNode, tree);
          if (resolvedLoop != null) {
            resolvedLoops.add(resolvedLoop);
          }
        }
      }

      // Ignored exceptional successors are not traversed: dataflow does not analyze a copy that
      // is reachable only through them, and the consistency analysis never reaches it.
      for (IPair<Block, @Nullable TypeMirror> successorAndExceptionType :
          CollectionOwnershipUtils.getSuccessorsExceptIgnoredExceptions(currentBlock, coAtf)) {
        Block successorBlock = successorAndExceptionType.first;
        if (successorBlock != null && visitedBlocks.add(successorBlock)) {
          worklist.addLast(successorBlock);
        }
      }
    }

    return resolvedLoops;
  }

  /**
   * Returns the resolved disposal loop info for the target enhanced-for-loop {@code tree} over a
   * resource collection.
   *
   * <p>Starting from the desugared iterator {@code next()} node (the only node whose {@link
   * MethodInvocationNode#getEnhancedForLoop()} is set), this walks forward to recover the
   * loop-variable assignment and body-entry block, then walks backward to recover the {@code
   * hasNext()} loop condition and the update block.
   *
   * @param methodInvocationNode the iterator {@code next()} invocation of {@code tree}
   * @param tree the enhanced-for-loop being resolved
   * @return the resolved loop info, or {@code null} if it cannot be resolved
   */
  private @Nullable DisposalLoopInfo resolveEnhancedForLoop(
      MethodInvocationNode methodInvocationNode, @FindDistinct EnhancedForLoopTree tree) {
    VariableTree loopVariable = tree.getVariable();

    // Walk forward from the iterator `next()` node to find the assignment that initializes the
    // loop variable from `next()`. The loop body starts right after that assignment, in the same
    // block.
    Block blockContainingNext = methodInvocationNode.getBlock();
    if (!(blockContainingNext instanceof SingleSuccessorBlock singleSuccessorBlock)) {
      return null;
    }
    Iterator<Node> nodeIterator = singleSuccessorBlock.getNodes().iterator();
    Node loopVariableNode = null;
    Node candidateNode;
    boolean isAssignmentOfLoopVariable;
    do {
      while (!nodeIterator.hasNext()) {
        Block successor = singleSuccessorBlock.getSuccessor();
        if (!(successor instanceof SingleSuccessorBlock nextBlock)) {
          return null;
        }
        singleSuccessorBlock = nextBlock;
        nodeIterator = singleSuccessorBlock.getNodes().iterator();
      }
      candidateNode = nodeIterator.next();
      isAssignmentOfLoopVariable = false;
      if ((candidateNode instanceof AssignmentNode)
          && (candidateNode.getTree() instanceof VariableTree iteratorVariableDeclaration)) {
        loopVariableNode = ((AssignmentNode) candidateNode).getTarget();
        isAssignmentOfLoopVariable =
            iteratorVariableDeclaration.getName() == loopVariable.getName();
      }
    } while (!isAssignmentOfLoopVariable);
    Block loopBodyEntryBlock = singleSuccessorBlock;

    // Walk backward from the iterator `next()` node to recover the `hasNext()` loop-condition node
    // for this desugared enhanced-for.
    Block currentBlock = blockContainingNext;
    nodeIterator = currentBlock.getNodes().iterator();
    Node loopConditionNode;
    boolean isLoopCondition;
    do {
      while (!nodeIterator.hasNext()) {
        Set<Block> predecessorBlocks = currentBlock.getPredecessors();
        if (predecessorBlocks.size() == 1) {
          currentBlock = predecessorBlocks.iterator().next();
          nodeIterator = currentBlock.getNodes().iterator();
        } else {
          return null;
        }
      }
      loopConditionNode = nodeIterator.next();
      isLoopCondition = false;
      if (loopConditionNode instanceof MethodInvocationNode) {
        MethodInvocationTree methodInvocationTree =
            ((MethodInvocationNode) loopConditionNode).getTree();
        isLoopCondition =
            methodInvocationTree != null && coAtf.isIteratorHasNextCall(methodInvocationTree);
      }
    } while (!isLoopCondition);

    Block blockContainingLoopCondition = loopConditionNode.getBlock();
    if (blockContainingLoopCondition == null) {
      return null;
    }
    // The `hasNext()` call is in an exception block, whose regular successor is the conditional
    // block and whose other successors are exceptional.
    if (!(blockContainingLoopCondition instanceof SingleSuccessorBlock loopConditionSsb)) {
      throw new BugInCF(
          "loop condition block is not a SingleSuccessorBlock, but: "
              + blockContainingLoopCondition.getClass());
    }
    Block maybeConditionalBlock = loopConditionSsb.getSuccessor();
    if (!(maybeConditionalBlock instanceof ConditionalBlock conditionalBlock)) {
      throw new BugInCF(
          "loop condition successor is not ConditionalBlock, but: "
              + (maybeConditionalBlock == null ? null : maybeConditionalBlock.getClass()));
    }
    // The loop-update (back-edge) block is the block that starts evaluating the loop condition:
    // the block that contains the receiver of `hasNext()`.  The `hasNext()` call itself is in a
    // later block, because the call can throw an exception.
    Block loopUpdateBlock =
        ((MethodInvocationNode) loopConditionNode).getTarget().getReceiver().getBlock();
    if (loopVariableNode == null || loopUpdateBlock == null) {
      return null;
    }
    Tree loopVariableTree = loopVariableNode.getTree();
    Tree loopConditionTree = loopConditionNode.getTree();
    if (loopVariableTree == null || loopConditionTree == null) {
      return null;
    }

    return new DisposalLoopInfo(
        tree.getExpression(),
        loopVariableTree,
        loopVariableNode,
        loopConditionTree,
        conditionalBlock,
        loopBodyEntryBlock,
        loopUpdateBlock);
  }

  /**
   * Returns whether {@code methodInvocationNode} is the iterator {@code next()} invocation for the
   * target enhanced-for-loop {@code tree}.
   *
   * @param methodInvocationNode the node to check
   * @param tree the target enhanced-for-loop
   * @return true if the node belongs to {@code tree}
   */
  private boolean isTargetEnhancedForInvocation(
      MethodInvocationNode methodInvocationNode, @FindDistinct EnhancedForLoopTree tree) {
    if (methodInvocationNode.getIterableExpression() == null) {
      return false;
    }

    EnhancedForLoopTree loop = methodInvocationNode.getEnhancedForLoop();
    return loop == tree;
  }
}
