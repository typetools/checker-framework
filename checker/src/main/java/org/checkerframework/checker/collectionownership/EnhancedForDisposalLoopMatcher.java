package org.checkerframework.checker.collectionownership;

import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.VariableTree;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
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
import org.checkerframework.javacutil.TreeUtils;
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
   * Returns the {@link DisposalLoopInfo} if the enhanced-for-loop iterates over a resource
   * collection.
   *
   * @param tree the enhanced-for-loop to inspect
   * @return the matched disposal loop info, or {@code null} if the loop does not match
   */
  @Nullable DisposalLoopInfo match(EnhancedForLoopTree tree) {
    ExpressionTree collectionTree =
        CollectionOwnershipUtils.referenceExpression(tree.getExpression());
    if (collectionTree == null) {
      return null;
    }
    if (!coAtf.isResourceCollection(collectionTree)) {
      return null;
    }
    return resolveEnhancedForLoop(tree);
  }

  /**
   * Searches the method CFG for the first occurrence of {@code tree} and resolves it to {@link
   * DisposalLoopInfo}.
   *
   * <p>This performs a CFG traversal, looking for a desugared iterator {@code hasNext()} node
   * tagged with the target enhanced-for-loop.
   *
   * @param tree the enhanced-for-loop to resolve
   * @return the CFG-resolved loop info, or {@code null} if it cannot be resolved
   */
  private @Nullable DisposalLoopInfo resolveEnhancedForLoop(EnhancedForLoopTree tree) {
    Block entryBlock = cfg.getEntryBlock();
    Set<Block> visitedBlocks = new HashSet<>();
    Deque<Block> worklist = new ArrayDeque<>();
    worklist.add(entryBlock);
    visitedBlocks.add(entryBlock);

    while (!worklist.isEmpty()) {
      Block currentBlock = worklist.removeFirst();
      for (Node node : currentBlock.getNodes()) {
        if (node instanceof MethodInvocationNode methodInvocationNode) {
          DisposalLoopInfo resolvedLoop = resolveEnhancedForLoop(methodInvocationNode, tree);
          if (resolvedLoop != null) {
            return resolvedLoop;
          }
        }
      }

      // One AST enhanced-for can have multiple CFG occurrences (for example around duplicated
      // finally paths for normal and exceptional flow). This resolver returns the first matching
      // occurrence it finds, so avoid traversing ignored exceptional successors here; otherwise
      // the search can bind to an exceptional clone before the normal one.
      for (IPair<Block, @Nullable TypeMirror> successorAndExceptionType :
          CollectionOwnershipUtils.getSuccessorsExceptIgnoredExceptions(currentBlock, coAtf)) {
        Block successorBlock = successorAndExceptionType.first;
        if (successorBlock != null && visitedBlocks.add(successorBlock)) {
          worklist.addLast(successorBlock);
        }
      }
    }

    return null;
  }

  /**
   * Returns resolved disposal loop info if the given node is desugared from the target
   * enhanced-for-loop {@code tree} over a resource collection.
   *
   * <p>Starting from the desugared iterator {@code hasNext()} node, this walks forward to recover
   * the loop-variable assignment and body-entry block, then walks backward to recover the loop
   * condition and update block.
   *
   * @param methodInvocationNode the node to check
   * @param tree the enhanced-for-loop being resolved
   * @return the resolved loop info, or {@code null} if the node does not belong to {@code tree}
   */
  private @Nullable DisposalLoopInfo resolveEnhancedForLoop(
      MethodInvocationNode methodInvocationNode, @FindDistinct EnhancedForLoopTree tree) {
    if (!isTargetEnhancedForInvocation(methodInvocationNode, tree)) {
      return null;
    }

    VariableTree loopVariable = tree.getVariable();

    // Walk forward from the iterator `hasNext()` node to find the assignment that initializes the
    // loop variable from `next()`. The successor after that assignment is the loop-body entry
    // block.
    Block blockContainingHasNext = methodInvocationNode.getBlock();
    if (!(blockContainingHasNext instanceof SingleSuccessorBlock singleSuccessorBlock)) {
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
    Block loopBodyEntryBlock = singleSuccessorBlock.getSuccessor();

    // Walk backward from the iterator `hasNext()` node to recover the loop-condition node and the
    // loop-update/back-edge block for this desugared enhanced-for.
    Block loopUpdateBlock = methodInvocationNode.getBlock();
    nodeIterator = loopUpdateBlock.getNodes().iterator();
    Node loopConditionNode;
    boolean isLoopCondition;
    do {
      while (!nodeIterator.hasNext()) {
        Set<Block> predecessorBlocks = loopUpdateBlock.getPredecessors();
        if (predecessorBlocks.size() == 1) {
          loopUpdateBlock = predecessorBlocks.iterator().next();
          nodeIterator = loopUpdateBlock.getNodes().iterator();
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
            methodInvocationTree != null && TreeUtils.isHasNextCall(methodInvocationTree);
      }
    } while (!isLoopCondition);

    Block blockContainingLoopCondition = loopConditionNode.getBlock();
    if (blockContainingLoopCondition.getSuccessors().size() != 1) {
      throw new BugInCF(
          "loop condition has: "
              + blockContainingLoopCondition.getSuccessors().size()
              + " successors instead of 1.");
    }
    Block maybeConditionalBlock = blockContainingLoopCondition.getSuccessors().iterator().next();
    if (!(maybeConditionalBlock instanceof ConditionalBlock conditionalBlock)) {
      throw new BugInCF(
          "loop condition successor is not ConditionalBlock, but: "
              + maybeConditionalBlock.getClass());
    }
    if (loopVariableNode == null
        || loopVariableNode.getTree() == null
        || loopConditionNode.getTree() == null) {
      return null;
    }

    return new DisposalLoopInfo(
        tree.getExpression(),
        loopVariableNode.getTree(),
        loopVariableNode,
        loopConditionNode.getTree(),
        conditionalBlock,
        loopBodyEntryBlock,
        loopUpdateBlock);
  }

  /**
   * Returns whether {@code methodInvocationNode} is the iterator {@code hasNext()} invocation for
   * the target enhanced-for-loop {@code tree}.
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
