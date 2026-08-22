package org.checkerframework.checker.collectionownership;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;
import org.checkerframework.dataflow.cfg.ControlFlowGraph;
import org.checkerframework.dataflow.cfg.UnderlyingAST;
import org.checkerframework.dataflow.cfg.block.Block;
import org.checkerframework.dataflow.cfg.block.ConditionalBlock;
import org.checkerframework.dataflow.cfg.node.Node;

/**
 * Stores the resolved CFG and AST facts for a potential disposal loop.
 *
 * <p>A potential disposal loop is a loop that iterates over a resource collection and whose shape
 * matches one of the supported collection iteration patterns recognized by {@link
 * DisposalLoopScanner}. This record stores the metadata for such loops which may or may not fulfill
 * the must-call obligations of the iterated element.
 *
 * <p>A potential disposal loop becomes a certified disposal loop only after later analysis
 * determines that the loop calls methods on the iterated element to satisfy its {@code @MustCall}
 * obligations. That process starts in {@link
 * CollectionOwnershipAnnotatedTypeFactory#postCFGConstruction(ControlFlowGraph, UnderlyingAST)},
 * which calls {@link
 * org.checkerframework.checker.resourceleak.MustCallConsistencyAnalyzer#analyzeDisposalLoop(
 * ControlFlowGraph, DisposalLoopInfo)} to compute the definitely called methods on the iterated
 * element. During later collection-ownership refinement, if those called methods satisfy the
 * iterated element's {@code @MustCall} obligations, the loop is treated as a certified disposal
 * loop.
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
    Block loopUpdateBlock) {}
