package org.checkerframework.checker.collectionownership;

import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.VariableTree;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import javax.lang.model.type.TypeMirror;
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

/** Resolves enhanced-`for` loops that may discharge collection obligations. */
final class EnhancedForDisposalLoopResolver {

  /** The CO type factory used for collection-ownership queries. */
  private final CollectionOwnershipAnnotatedTypeFactory atypeFactory;

  /** The CFG of the method currently being scanned. */
  private final ControlFlowGraph cfg;

  /** The method currently being scanned, or {@code null} if the CFG is not for a method. */
  private final @Nullable MethodTree methodTree;

  /**
   * Creates a resolver for enhanced-`for` disposal loops.
   *
   * @param atypeFactory the CO type factory
   * @param cfg the CFG being scanned
   * @param methodTree the enclosing method, or {@code null}
   */
  EnhancedForDisposalLoopResolver(
      CollectionOwnershipAnnotatedTypeFactory atypeFactory,
      ControlFlowGraph cfg,
      @Nullable MethodTree methodTree) {
    this.atypeFactory = atypeFactory;
    this.cfg = cfg;
    this.methodTree = methodTree;
  }

  /**
   * Adds an enhanced-for-loop that fulfills collection obligations.
   *
   * @param tree the enhanced-for-loop to inspect
   * @return the matched disposal loop, or {@code null} if the loop does not match
   */
  @Nullable DisposalLoop match(EnhancedForLoopTree tree) {
    MethodTree enclosingMethodTree =
        CollectionOwnershipUtils.getEnclosingMethodForCollectionLoop(methodTree);
    if (enclosingMethodTree == null) {
      return null;
    }
    ExpressionTree collectionTree =
        CollectionOwnershipUtils.collectionTreeFromExpression(tree.getExpression());
    if (collectionTree == null) {
      return null;
    }
    if (!atypeFactory.isResourceCollection(collectionTree)) {
      return null;
    }
    return resolveEnhancedForLoop(tree);
  }

  /**
   * Resolves an enhanced-for-loop candidate into a CFG-resolved loop.
   *
   * @param tree the enhanced-for-loop to resolve
   * @return the CFG-resolved loop, or {@code null} if it cannot be resolved
   */
  private @Nullable DisposalLoop resolveEnhancedForLoop(EnhancedForLoopTree tree) {
    Block entryBlock = cfg.getEntryBlock();
    Set<Block> visitedBlocks = new HashSet<>();
    Deque<Block> worklist = new ArrayDeque<>();
    worklist.add(entryBlock);
    visitedBlocks.add(entryBlock);

    while (!worklist.isEmpty()) {
      Block currentBlock = worklist.removeFirst();

      for (Node node : currentBlock.getNodes()) {
        if (node instanceof MethodInvocationNode) {
          DisposalLoop resolvedLoop = resolveEnhancedForLoop((MethodInvocationNode) node, tree);
          if (resolvedLoop != null) {
            return resolvedLoop;
          }
        }
      }

      for (IPair<Block, @Nullable TypeMirror> successorAndExceptionType :
          CollectionOwnershipUtils.getSuccessorsExceptIgnoredExceptions(
              currentBlock, atypeFactory)) {
        Block successorBlock = successorAndExceptionType.first;
        if (successorBlock != null && visitedBlocks.add(successorBlock)) {
          worklist.addLast(successorBlock);
        }
      }
    }

    return null;
  }

  /**
   * Returns a resolved collection loop if the given node is desugared from an enhanced-for-loop
   * over a collection.
   *
   * @param methodInvocationNode the node to check
   * @param tree the enhanced-for-loop being resolved
   * @return the resolved loop, or {@code null} if the node does not belong to {@code tree}
   */
  private @Nullable DisposalLoop resolveEnhancedForLoop(
      MethodInvocationNode methodInvocationNode, EnhancedForLoopTree tree) {
    if (methodInvocationNode.getIterableExpression() == null) {
      return null;
    }

    EnhancedForLoopTree loop = methodInvocationNode.getEnhancedForLoop();
    if (loop == null) {
      throw new BugInCF(
          "MethodInvocationNode.iterableExpression should be non-null iff"
              + " MethodInvocationNode.enhancedForLoop is non-null");
    }
    if (loop != tree) {
      return null;
    }

    VariableTree loopVariable = loop.getVariable();

    SingleSuccessorBlock singleSuccessorBlock =
        (SingleSuccessorBlock) methodInvocationNode.getBlock();
    Iterator<Node> nodeIterator = singleSuccessorBlock.getNodes().iterator();
    Node loopVariableNode = null;
    Node node;
    boolean isAssignmentOfLoopVariable;
    do {
      while (!nodeIterator.hasNext()) {
        singleSuccessorBlock = (SingleSuccessorBlock) singleSuccessorBlock.getSuccessor();
        nodeIterator = singleSuccessorBlock.getNodes().iterator();
      }
      node = nodeIterator.next();
      isAssignmentOfLoopVariable = false;
      if ((node instanceof AssignmentNode)
          && (node.getTree() instanceof VariableTree iteratorVariableDeclaration)) {
        loopVariableNode = ((AssignmentNode) node).getTarget();
        isAssignmentOfLoopVariable =
            iteratorVariableDeclaration.getName() == loopVariable.getName();
      }
    } while (!isAssignmentOfLoopVariable);
    Block loopBodyEntryBlock = singleSuccessorBlock.getSuccessor();

    Block loopUpdateBlock = methodInvocationNode.getBlock();
    nodeIterator = loopUpdateBlock.getNodes().iterator();
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
      node = nodeIterator.next();
      isLoopCondition = false;
      if (node instanceof MethodInvocationNode) {
        MethodInvocationTree methodInvocationTree = ((MethodInvocationNode) node).getTree();
        isLoopCondition =
            methodInvocationTree != null && TreeUtils.isHasNextCall(methodInvocationTree);
      }
    } while (!isLoopCondition);

    Block blockContainingLoopCondition = node.getBlock();
    if (blockContainingLoopCondition.getSuccessors().size() != 1) {
      throw new BugInCF(
          "loop condition has: "
              + blockContainingLoopCondition.getSuccessors().size()
              + " successors instead of 1.");
    }
    Block conditionalBlock = blockContainingLoopCondition.getSuccessors().iterator().next();
    if (!(conditionalBlock instanceof ConditionalBlock)) {
      throw new BugInCF(
          "loop condition successor is not ConditionalBlock, but: " + conditionalBlock.getClass());
    }
    if (loopVariableNode == null || loopVariableNode.getTree() == null || node.getTree() == null) {
      return null;
    }

    return new DisposalLoop(
        loop.getExpression(),
        loopVariableNode.getTree(),
        loopVariableNode,
        node.getTree(),
        (ConditionalBlock) conditionalBlock,
        loopBodyEntryBlock,
        loopUpdateBlock);
  }
}
