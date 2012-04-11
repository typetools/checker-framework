package checkers.flow.analysis;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Map.Entry;

import checkers.flow.cfg.node.Node;

import com.sun.source.tree.Tree;

/**
 * An {@link AnalysisResult} represents the result of a dataflow analysis by
 * providing the abstract values given a node or a tree. Note that it does not
 * keep track of custom results computed by some analyses.
 * 
 * @author Stefan Heule
 * 
 * @param <A>
 *            type of the abstract value that is tracked.
 */
public class AnalysisResult<A extends AbstractValue<A>> {

    /** Abstract values of nodes. */
    final protected Map<Node, A> nodeValues;

    /** Abstract values of tree nodes (used as cache). */
    final protected Map<Tree, A> treeValues;
    
    /** Map from AST {@link Tree}s to {@link Node}s. */
    final protected Map<Tree, Node> treeLookup;

    /**
     * Initialize with a given node-value mapping.
     */
    public AnalysisResult(Map<Node, A> nodeValues, Map<Tree, Node> treeLookup) {
        this.nodeValues = new IdentityHashMap<>(nodeValues);
        treeValues = new IdentityHashMap<>();
        this.treeLookup = new IdentityHashMap<>(treeLookup);
    }

    /**
     * Initialize empty result.
     */
    public AnalysisResult() {
        nodeValues = new IdentityHashMap<>();
        treeValues = new IdentityHashMap<>();
        treeLookup = new IdentityHashMap<>();
    }

    /**
     * Combine with another analysis result.
     */
    public void combine(AnalysisResult<A> other) {
        for (Entry<Node, A> e : other.nodeValues.entrySet()) {
            nodeValues.put(e.getKey(), e.getValue());
        }
        for (Entry<Tree, A> e : other.treeValues.entrySet()) {
            treeValues.put(e.getKey(), e.getValue());
        }
        for (Entry<Tree, Node> e : other.treeLookup.entrySet()) {
            treeLookup.put(e.getKey(), e.getValue());
        }
    }

    /**
     * @return The abstract value for {@link Node} {@code n}, or {@code null} if
     *         no information is available.
     */
    public/* @Nullable */A getValue(Node n) {
        return nodeValues.get(n);
    }

    /**
     * @return The abstract value for {@link Tree} {@code t}, or {@code null} if
     *         no information is available.
     */
    public/* @Nullable */A getValue(Tree t) {
        if (treeValues.containsKey(t)) {
            return treeValues.get(t);
        }
        A val = getValue(treeLookup.get(t));
        treeValues.put(t, val);
        return val;
    }
}
