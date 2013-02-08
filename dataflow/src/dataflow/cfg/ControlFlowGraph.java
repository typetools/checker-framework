package dataflow.cfg;

import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import dataflow.cfg.block.Block;
import dataflow.cfg.block.Block.BlockType;
import dataflow.cfg.block.ConditionalBlock;
import dataflow.cfg.block.ExceptionBlock;
import dataflow.cfg.block.SingleSuccessorBlock;
import dataflow.cfg.block.SpecialBlock;
import dataflow.cfg.block.SpecialBlockImpl;
import dataflow.cfg.node.Node;
import dataflow.cfg.node.ReturnNode;

import com.sun.source.tree.Tree;

/**
 * A control flow graph (CFG for short) of a single method.
 *
 * @author Stefan Heule
 *
 */
public class ControlFlowGraph {

    /** The entry block of the control flow graph. */
    protected final SpecialBlock entryBlock;

    /** The regular exit block of the control flow graph. */
    protected final SpecialBlock regularExitBlock;

    /** The exceptiona exit block of the control flow graph. */
    protected final SpecialBlock exceptionalExitBlock;

    /** The AST this CFG corresponds to. */
    protected UnderlyingAST underlyingAST;

    /** Map from AST {@link Tree}s to {@link Node}s. */
    protected IdentityHashMap<Tree, Node> treeLookup;

    /**
     * All return nodes (if any) encountered. Only includes return
     * statements that actually return something
     */
    protected final List<ReturnNode> returnNodes;

    public ControlFlowGraph(SpecialBlock entryBlock, SpecialBlockImpl regularExitBlock, SpecialBlockImpl exceptionalExitBlock, UnderlyingAST underlyingAST,
            IdentityHashMap<Tree, Node> treeLookup, List<ReturnNode> returnNodes) {
        super();
        this.entryBlock = entryBlock;
        this.underlyingAST = underlyingAST;
        this.treeLookup = treeLookup;
        this.regularExitBlock = regularExitBlock;
        this.exceptionalExitBlock = exceptionalExitBlock;
        this.returnNodes = returnNodes;
    }

    /**
     * @return The {@link Node} to which the {@link Tree} <code>t</code>
     *         corresponds.
     */
    public Node getNodeCorrespondingToTree(Tree t) {
        return treeLookup.get(t);
    }

    /** @return The entry block of the control flow graph. */
    public SpecialBlock getEntryBlock() {
        return entryBlock;
    }

    public List<ReturnNode> getReturnNodes() {
        return returnNodes;
    }

    public SpecialBlock getRegularExitBlock() {
        return regularExitBlock;
    }

    public SpecialBlock getExceptionalExitBlock() {
        return exceptionalExitBlock;
    }

    /** @return The AST this CFG corresponds to. */
    public UnderlyingAST getUnderlyingAST() {
        return underlyingAST;
    }

    /**
     * @return The list of all basic block in this control flow graph.
     */
    public Set<Block> getAllBlocks() {
        Set<Block> visited = new HashSet<>();
        Queue<Block> worklist = new LinkedList<>();
        Block cur = entryBlock;
        visited.add(entryBlock);

        // traverse the whole control flow graph
        while (true) {
            if (cur == null)
                break;

            Queue<Block> succs = new LinkedList<>();
            if (cur.getType() == BlockType.CONDITIONAL_BLOCK) {
                ConditionalBlock ccur = ((ConditionalBlock) cur);
                succs.add(ccur.getThenSuccessor());
                succs.add(ccur.getElseSuccessor());
            } else {
                assert cur instanceof SingleSuccessorBlock;
                Block b = ((SingleSuccessorBlock) cur).getSuccessor();
                if (b != null) {
                    succs.add(b);
                }
            }

            if (cur.getType() == BlockType.EXCEPTION_BLOCK) {
                ExceptionBlock ecur = (ExceptionBlock) cur;
                succs.addAll(ecur.getExceptionalSuccessors().values());
            }

            for (Block b : succs) {
                if (!visited.contains(b)) {
                    visited.add(b);
                    worklist.add(b);
                }
            }

            cur = worklist.poll();
        }

        return visited;
    }

    /**
     * @return The tree-lookup map.
     */
    public IdentityHashMap<Tree, Node> getTreeLookup() {
        return new IdentityHashMap<>(treeLookup);
    }

}
