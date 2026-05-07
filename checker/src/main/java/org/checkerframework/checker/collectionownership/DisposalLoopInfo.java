package org.checkerframework.checker.collectionownership;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;
import java.util.Objects;
import org.checkerframework.dataflow.cfg.block.Block;
import org.checkerframework.dataflow.cfg.block.ConditionalBlock;
import org.checkerframework.dataflow.cfg.node.Node;

/**
 * Stores the resolved CFG and AST facts for a potential disposal loop. A disposal loop is a loop
 * that iterates over a resource collection and may call the disposal method, e.g., close() on the
 * iterated resource. This record stores the metadata for disposal loops which may or may not
 * fullfill the must-call obligations of the iterated element.
 *
 * @param expressionTree The {@code ExpressionTree} for collection that this loop iterates over.
 * @param iteratedElementTree The {@code Tree} for the iterated collection element by this loop.
 * @param iteratedElementNode The CFG {@code Node} for the iterated collection element by this loop.
 * @param loopConditionTree The condition {@code Tree} for this loop.
 * @param loopConditionalBlock The conditional {@code Block} corresponding to the loop condition.
 * @param loopBodyEntryBlock The entry {@code Block} for this loop's body.
 * @param loopUpdateBlock The loop-update {@code Block}.
 */
public record DisposalLoopInfo(
    ExpressionTree expressionTree,
    Tree iteratedElementTree,
    Node iteratedElementNode,
    Tree loopConditionTree,
    ConditionalBlock loopConditionalBlock,
    Block loopBodyEntryBlock,
    Block loopUpdateBlock) {

  /**
   * Constructs a new {@code DisposalLoopInfo}.
   *
   * @param expressionTree the {@code ExpressionTree} for the collection that this loop iterates
   *     over
   * @param iteratedElementTree the {@code Tree} for the iterated collection element
   * @param iteratedElementNode the CFG {@code Node} for the iterated collection element
   * @param loopConditionTree the condition {@code Tree} for this loop
   * @param loopConditionalBlock the conditional {@code Block} corresponding to the loop condition
   * @param loopBodyEntryBlock the entry {@code Block} for this loop's body
   * @param loopUpdateBlock the loop-update {@code Block}
   */
  public DisposalLoopInfo(
      ExpressionTree expressionTree,
      Tree iteratedElementTree,
      Node iteratedElementNode,
      Tree loopConditionTree,
      ConditionalBlock loopConditionalBlock,
      Block loopBodyEntryBlock,
      Block loopUpdateBlock) {
    this.expressionTree = Objects.requireNonNull(expressionTree);
    this.iteratedElementTree = Objects.requireNonNull(iteratedElementTree);
    this.iteratedElementNode = Objects.requireNonNull(iteratedElementNode);
    this.loopConditionTree = Objects.requireNonNull(loopConditionTree);
    this.loopConditionalBlock = Objects.requireNonNull(loopConditionalBlock);
    this.loopBodyEntryBlock = Objects.requireNonNull(loopBodyEntryBlock);
    this.loopUpdateBlock = Objects.requireNonNull(loopUpdateBlock);
  }
}
