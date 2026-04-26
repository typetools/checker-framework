package org.checkerframework.checker.collectionownership;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.cfg.block.Block;
import org.checkerframework.dataflow.cfg.block.ConditionalBlock;
import org.checkerframework.dataflow.cfg.node.Node;

/** Coordinates CO-owned state for disposal loops and their MCCA proof results. */
public final class DisposalLoopCoordinator {

  /** The Collection Ownership type factory that owns this coordinator. */
  @SuppressWarnings("unused")
  private final CollectionOwnershipAnnotatedTypeFactory coAtf;

  /** Map from a loop-condition {@code Tree} to its corresponding {@link DisposalLoop}. */
  private final IdentityHashMap<Tree, DisposalLoop> conditionToDisposalLoopMap = new IdentityHashMap<>();

  /** Map from a loop's conditional {@code Block} to its corresponding {@link DisposalLoop} */
  private final IdentityHashMap<Block, DisposalLoop> conditionalBlockToDisposalLoopMap =
      new IdentityHashMap<>();

  /** Map from a {@link DisposalLoop} to the called-methods proven by MCCA for that loop. */
  private final IdentityHashMap<DisposalLoop, Set<String>> disposalLoopToProvenCalledMethodsMap =
      new IdentityHashMap<>();

  /**
   * Creates a coordinator for disposal loops.
   *
   * @param coAtf the Collection Ownership type factory that owns this coordinator
   */
  public DisposalLoopCoordinator(CollectionOwnershipAnnotatedTypeFactory coAtf) {
    this.coAtf = Objects.requireNonNull(coAtf);
  }

  /**
   * Wrapper for a loop that iterates over a resource collection and may call methods on the
   * iterated collection element. This class stores the loop metadata needed for CO transfer and
   * MCCA proof.
   */
  public static class DisposalLoop {

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

  /**
   * Returns the {@link DisposalLoop} corresponding to the loop condition {@code tree}, if one
   * exists.
   *
   * @param tree the condition tree
   * @return the {@link DisposalLoop} for condition {@code tree} if exists, otherwise {@code null}.
   */
  public @Nullable DisposalLoop getDisposalLoopForConditionTree(Tree tree) {
    return conditionToDisposalLoopMap.get(tree);
  }

  /**
   * Returns the {@link DisposalLoop} corresponding to the loop conditional {@code block}, if one
   * exists.
   *
   * @param block the loop-condition block
   * @return the {@link DisposalLoop} for conditional {@code block} if exists, otherwise {@code
   *     null}.
   */
  public @Nullable DisposalLoop getDisposalLoopForConditionBlock(Block block) {
    return conditionalBlockToDisposalLoopMap.get(block);
  }

  /**
   * Returns the called-methods proven by MCCA for a disposal loop.
   *
   * @param disposalLoop the disposal loop
   * @return the methods proven for {@code disposalLoop}, or {@code null} if none are populated.
   */
  public @Nullable Set<String> getProvenCalledMethods(DisposalLoop disposalLoop) {
    return disposalLoopToProvenCalledMethodsMap.get(disposalLoop);
  }

  /**
   * Registers a disposal loop whose proof succeeded.
   *
   * @param disposalLoop the disposal loop
   * @param provenCalledMethods the methods proven by MCCA for the disposal loop
   */
  public void registerProvenDisposalLoop(
      DisposalLoop disposalLoop, Set<String> provenCalledMethods) {
    Objects.requireNonNull(disposalLoop);
    Objects.requireNonNull(provenCalledMethods);

    conditionToDisposalLoopMap.put(disposalLoop.loopConditionTree, disposalLoop);
    conditionalBlockToDisposalLoopMap.put(disposalLoop.loopConditionalBlock, disposalLoop);
    disposalLoopToProvenCalledMethodsMap.put(
        disposalLoop, Collections.unmodifiableSet(new LinkedHashSet<>(provenCalledMethods)));
  }

  /** Clears all disposal loops and MCCA proof results in this coordinator. */
  public void clear() {
    conditionToDisposalLoopMap.clear();
    conditionalBlockToDisposalLoopMap.clear();
    disposalLoopToProvenCalledMethodsMap.clear();
  }
}
