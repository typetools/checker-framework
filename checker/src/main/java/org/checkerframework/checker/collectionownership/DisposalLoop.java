package org.checkerframework.checker.collectionownership;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;
import java.util.Objects;
import org.checkerframework.dataflow.cfg.block.Block;
import org.checkerframework.dataflow.cfg.block.ConditionalBlock;
import org.checkerframework.dataflow.cfg.node.Node;

/**
 * Stores the resolved CFG and AST facts for a disposal loop. A disposal loop is a loop that
 * iterates over a resource collection and may call the disposal method, e.g., close() on the
 * iterated resource.
 */
public class DisposalLoop {

  /** The {@code ExpressionTree} for collection that this loop iterates over. */
  public final ExpressionTree expressionTree;

  /** The {@code Tree} for the iterated collection element by this loop. */
  public final Tree iteratedElementTree;

  /** The CFG {@code Node} for the iterated collection element by this loop. */
  public final Node iteratedElementNode;

  /** The condition {@code Tree} for this loop. */
  public final Tree loopConditionTree;

  /** The conditional {@code Block} corresponding to the loop condition. */
  public final ConditionalBlock loopConditionalBlock;

  /** The entry {@code Block} for this loop's body. */
  public final Block loopBodyEntryBlock;

  /** The loop-update {@code Block}. */
  public final Block loopUpdateBlock;

  /**
   * Constructs a new {@code DisposalLoop}.
   *
   * @param collectionExpressionTree the {@code ExpressionTree} for the collection that this loop
   *     iterates over
   * @param iteratedElementTree the {@code Tree} for the iterated collection element
   * @param iteratedElementNode the CFG {@code Node} for the iterated collection element
   * @param loopConditionTree the condition {@code Tree} for this loop
   * @param loopConditionBlock the conditional {@code Block} corresponding to the loop condition
   * @param loopBodyEntryBlock the entry {@code Block} for this loop's body
   * @param loopUpdateBlock the loop-update {@code Block}
   */
  public DisposalLoop(
      ExpressionTree collectionExpressionTree,
      Tree iteratedElementTree,
      Node iteratedElementNode,
      Tree loopConditionTree,
      ConditionalBlock loopConditionBlock,
      Block loopBodyEntryBlock,
      Block loopUpdateBlock) {
    this.expressionTree = Objects.requireNonNull(collectionExpressionTree);
    this.iteratedElementTree = Objects.requireNonNull(iteratedElementTree);
    this.iteratedElementNode = Objects.requireNonNull(iteratedElementNode);
    this.loopConditionTree = Objects.requireNonNull(loopConditionTree);
    this.loopConditionalBlock = Objects.requireNonNull(loopConditionBlock);
    this.loopBodyEntryBlock = Objects.requireNonNull(loopBodyEntryBlock);
    this.loopUpdateBlock = Objects.requireNonNull(loopUpdateBlock);
  }
}
