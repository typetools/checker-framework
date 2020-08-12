package org.checkerframework.dataflow.cfg;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.UnaryTree;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.StringJoiner;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.analysis.AnalysisResult;
import org.checkerframework.dataflow.cfg.block.Block;
import org.checkerframework.dataflow.cfg.block.ConditionalBlock;
import org.checkerframework.dataflow.cfg.block.ExceptionBlock;
import org.checkerframework.dataflow.cfg.block.RegularBlock;
import org.checkerframework.dataflow.cfg.block.SingleSuccessorBlock;
import org.checkerframework.dataflow.cfg.block.SpecialBlock;
import org.checkerframework.dataflow.cfg.block.SpecialBlockImpl;
import org.checkerframework.dataflow.cfg.node.AssignmentNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.ReturnNode;

/**
 * A control flow graph (CFG for short) of a single method.
 *
 * <p>The graph is represented by the successors (methods {@link SingleSuccessorBlock#getSuccessor},
 * {@link ConditionalBlock#getThenSuccessor}, {@link ConditionalBlock#getElseSuccessor}, {@link
 * ExceptionBlock#getExceptionalSuccessors}, {@link RegularBlock#getRegularSuccessor}) and
 * predecessors (method {@link Block#getPredecessors}) of the entry and exit blocks.
 */
public class ControlFlowGraph {

    /** The entry block of the control flow graph. */
    protected final SpecialBlock entryBlock;

    /** The regular exit block of the control flow graph. */
    protected final SpecialBlock regularExitBlock;

    /** The exceptional exit block of the control flow graph. */
    protected final SpecialBlock exceptionalExitBlock;

    /** The AST this CFG corresponds to. */
    protected final UnderlyingAST underlyingAST;

    /**
     * Maps from AST {@link Tree}s to sets of {@link Node}s.
     *
     * <ul>
     *   <li>Most Trees that produce a value will have at least one corresponding Node.
     *   <li>Trees that undergo conversions, such as boxing or unboxing, can map to two distinct
     *       Nodes. The Node for the pre-conversion value is stored in {@link #treeLookup}, while
     *       the Node for the post-conversion value is stored in {@link #convertedTreeLookup}.
     * </ul>
     *
     * Some of the mapped-to nodes (in both {@link #treeLookup} and {@link #convertedTreeLookup}) do
     * not appear in {@link #getAllNodes} because their blocks are not reachable in the control flow
     * graph. Dataflow will not compute abstract values for these nodes.
     */
    protected final IdentityHashMap<Tree, Set<Node>> treeLookup;

    /** Map from AST {@link Tree}s to post-conversion sets of {@link Node}s. */
    protected final IdentityHashMap<Tree, Set<Node>> convertedTreeLookup;

    /** Map from AST {@link UnaryTree}s to corresponding {@link AssignmentNode}s. */
    protected final IdentityHashMap<UnaryTree, AssignmentNode> unaryAssignNodeLookup;

    /**
     * All return nodes (if any) encountered. Only includes return statements that actually return
     * something
     */
    protected final List<ReturnNode> returnNodes;

    /**
     * Class declarations that have been encountered when building the control-flow graph for a
     * method.
     */
    protected final List<ClassTree> declaredClasses;

    /**
     * Lambdas encountered when building the control-flow graph for a method, variable initializer,
     * or initializer.
     */
    protected final List<LambdaExpressionTree> declaredLambdas;

    public ControlFlowGraph(
            SpecialBlock entryBlock,
            SpecialBlockImpl regularExitBlock,
            SpecialBlockImpl exceptionalExitBlock,
            UnderlyingAST underlyingAST,
            IdentityHashMap<Tree, Set<Node>> treeLookup,
            IdentityHashMap<Tree, Set<Node>> convertedTreeLookup,
            IdentityHashMap<UnaryTree, AssignmentNode> unaryAssignNodeLookup,
            List<ReturnNode> returnNodes,
            List<ClassTree> declaredClasses,
            List<LambdaExpressionTree> declaredLambdas) {
        super();
        this.entryBlock = entryBlock;
        this.underlyingAST = underlyingAST;
        this.treeLookup = treeLookup;
        this.unaryAssignNodeLookup = unaryAssignNodeLookup;
        this.convertedTreeLookup = convertedTreeLookup;
        this.regularExitBlock = regularExitBlock;
        this.exceptionalExitBlock = exceptionalExitBlock;
        this.returnNodes = returnNodes;
        this.declaredClasses = declaredClasses;
        this.declaredLambdas = declaredLambdas;
    }

    /**
     * Returns the set of {@link Node}s to which the {@link Tree} {@code t} corresponds, or null for
     * trees that don't produce a value.
     *
     * @param t a tree
     * @return the set of {@link Node}s to which the {@link Tree} {@code t} corresponds, or null for
     *     trees that don't produce a value
     */
    public @Nullable Set<Node> getNodesCorrespondingToTree(Tree t) {
        if (convertedTreeLookup.containsKey(t)) {
            return convertedTreeLookup.get(t);
        } else {
            return treeLookup.get(t);
        }
    }

    /**
     * Returns the entry block of the control flow graph.
     *
     * @return the entry block of the control flow graph
     */
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

    /**
     * Returns the AST this CFG corresponds to.
     *
     * @return the AST this CFG corresponds to
     */
    public UnderlyingAST getUnderlyingAST() {
        return underlyingAST;
    }

    /**
     * Returns the set of all basic blocks in this control flow graph.
     *
     * @return the set of all basic blocks in this control flow graph
     */
    public Set<Block> getAllBlocks(
            @UnknownInitialization(ControlFlowGraph.class) ControlFlowGraph this) {
        Set<Block> visited = new HashSet<>();
        Queue<Block> worklist = new ArrayDeque<>();
        Block cur = entryBlock;
        visited.add(entryBlock);

        // traverse the whole control flow graph
        while (true) {
            if (cur == null) {
                break;
            }

            Collection<Block> succs = cur.getSuccessors();

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
     * Returns all nodes in this control flow graph.
     *
     * @return all nodes in this control flow graph
     */
    public List<Node> getAllNodes(
            @UnknownInitialization(ControlFlowGraph.class) ControlFlowGraph this) {
        List<Node> result = new ArrayList<>();
        for (Block b : getAllBlocks()) {
            result.addAll(b.getNodes());
        }
        return result;
    }

    /**
     * Returns all basic blocks in this control flow graph, in reversed depth-first postorder.
     * Blocks may appear more than once in the sequence.
     *
     * @return the list of all basic block in this control flow graph in reversed depth-first
     *     postorder sequence
     */
    public List<Block> getDepthFirstOrderedBlocks() {
        List<Block> dfsOrderResult = new ArrayList<>();
        Set<Block> visited = new HashSet<>();
        Deque<Block> worklist = new ArrayDeque<>();
        worklist.add(entryBlock);
        while (!worklist.isEmpty()) {
            Block cur = worklist.getLast();
            if (visited.contains(cur)) {
                dfsOrderResult.add(cur);
                worklist.removeLast();
            } else {
                visited.add(cur);
                Collection<Block> successors = cur.getSuccessors();
                successors.removeAll(visited);
                worklist.addAll(successors);
            }
        }

        Collections.reverse(dfsOrderResult);
        return dfsOrderResult;
    }

    /**
     * Returns the copied tree-lookup map. Ignores convertedTreeLookup, though {@link
     * #getNodesCorrespondingToTree} uses that field.
     *
     * @return the copied tree-lookup map
     */
    public IdentityHashMap<Tree, Set<Node>> getTreeLookup() {
        return new IdentityHashMap<>(treeLookup);
    }

    /**
     * Returns the copied lookup-map of the assign node for unary operation.
     *
     * @return the copied lookup-map of the assign node for unary operation
     */
    public IdentityHashMap<UnaryTree, AssignmentNode> getUnaryAssignNodeLookup() {
        return new IdentityHashMap<>(unaryAssignNodeLookup);
    }

    /**
     * Get the {@link MethodTree} of the CFG if the argument {@link Tree} maps to a {@link Node} in
     * the CFG or null otherwise.
     */
    public @Nullable MethodTree getContainingMethod(Tree t) {
        if (treeLookup.containsKey(t)) {
            if (underlyingAST.getKind() == UnderlyingAST.Kind.METHOD) {
                UnderlyingAST.CFGMethod cfgMethod = (UnderlyingAST.CFGMethod) underlyingAST;
                return cfgMethod.getMethod();
            }
        }
        return null;
    }

    /**
     * Get the {@link ClassTree} of the CFG if the argument {@link Tree} maps to a {@link Node} in
     * the CFG or null otherwise.
     */
    public @Nullable ClassTree getContainingClass(Tree t) {
        if (treeLookup.containsKey(t)) {
            if (underlyingAST.getKind() == UnderlyingAST.Kind.METHOD) {
                UnderlyingAST.CFGMethod cfgMethod = (UnderlyingAST.CFGMethod) underlyingAST;
                return cfgMethod.getClassTree();
            }
        }
        return null;
    }

    public List<ClassTree> getDeclaredClasses() {
        return declaredClasses;
    }

    public List<LambdaExpressionTree> getDeclaredLambdas() {
        return declaredLambdas;
    }

    @Override
    public String toString() {
        Map<String, Object> args = new HashMap<>();
        args.put("verbose", true);

        CFGVisualizer<?, ?, ?> viz = new StringCFGVisualizer<>();
        viz.init(args);
        Map<String, Object> res = viz.visualize(this, this.getEntryBlock(), null);
        viz.shutdown();
        if (res == null) {
            return super.toString();
        }
        String stringGraph = (String) res.get("stringGraph");
        return stringGraph == null ? super.toString() : stringGraph;
    }

    /**
     * Returns a verbose string representation of this, useful for debugging.
     *
     * @return a string representation of this
     */
    public String toStringDebug() {
        String className = this.getClass().getSimpleName();
        if (className.equals("ControlFlowGraph") && this.getClass() != ControlFlowGraph.class) {
            className = this.getClass().getCanonicalName();
        }

        StringJoiner result = new StringJoiner(String.format("%n  "));
        result.add(className + "{");
        result.add("entryBlock=" + entryBlock);
        result.add("regularExitBlock=" + regularExitBlock);
        result.add("exceptionalExitBlock=" + exceptionalExitBlock);
        String astString = underlyingAST.toString().replaceAll("\\s", " ");
        if (astString.length() > 65) {
            astString = "\"" + astString.substring(0, 60) + "\"";
        }
        result.add("underlyingAST=" + underlyingAST);
        result.add("treeLookup=" + AnalysisResult.treeLookupToString(treeLookup));
        result.add("convertedTreeLookup=" + AnalysisResult.treeLookupToString(convertedTreeLookup));
        result.add("unaryAssignNodeLookup=" + unaryAssignNodeLookup);
        result.add("returnNodes=" + Node.nodeCollectionToString(returnNodes));
        result.add("declaredClasses=" + declaredClasses);
        result.add("declaredLambdas=" + declaredLambdas);
        result.add("}");
        return result.toString();
    }
}
