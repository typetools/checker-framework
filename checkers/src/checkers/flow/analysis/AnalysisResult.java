package checkers.flow.analysis;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Map.Entry;

import checkers.flow.cfg.node.MethodInvocationNode;
import checkers.flow.cfg.node.Node;

import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;

/**
 * An {@link AnalysisResult} represents the result of a dataflow analysis by
 * providing the abstract values given a node or a tree. Note that it does not
 * keep track of custom results computed by some analysis.
 *
 * @author Stefan Heule
 *
 * @param <A>
 *            type of the abstract value that is tracked.
 */
public class AnalysisResult<A extends AbstractValue<A>, S extends Store<S>> {

    /** Abstract values of nodes. */
    protected final Map<Node, A> nodeValues;

    /** Map from AST {@link Tree}s to {@link Node}s. */
    protected final Map<Tree, Node> treeLookup;

    /**
     * The stores before every method call.
     */
    protected final Map<MethodInvocationNode, S> storesBeforeMethodInvocation;

    /**
     * Initialize with a given node-value mapping.
     */
    public AnalysisResult(Map<Node, A> nodeValues,
            Map<MethodInvocationNode, S> storesBeforeMethodInvocation,
            Map<Tree, Node> treeLookup) {
        this.nodeValues = new IdentityHashMap<>(nodeValues);
        this.treeLookup = new IdentityHashMap<>(treeLookup);
        this.storesBeforeMethodInvocation = storesBeforeMethodInvocation;
    }

    /**
     * Initialize empty result.
     */
    public AnalysisResult() {
        nodeValues = new IdentityHashMap<>();
        treeLookup = new IdentityHashMap<>();
        storesBeforeMethodInvocation = new IdentityHashMap<>();
    }

    /**
     * Combine with another analysis result.
     */
    public void combine(AnalysisResult<A, S> other) {
        for (Entry<Node, A> e : other.nodeValues.entrySet()) {
            nodeValues.put(e.getKey(), e.getValue());
        }
        for (Entry<Tree, Node> e : other.treeLookup.entrySet()) {
            treeLookup.put(e.getKey(), e.getValue());
        }
        for (Entry<MethodInvocationNode, S> e : other.storesBeforeMethodInvocation
                .entrySet()) {
            storesBeforeMethodInvocation.put(e.getKey(), e.getValue());
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
        A val = getValue(treeLookup.get(t));
        return val;
    }

    /**
     * @return The {@link Node} for a given {@link Tree}.
     */
    public/* @Nullable */Node getNodeForTree(Tree tree) {
        return treeLookup.get(tree);
    }

    /**
     * @return The store immediately before a method invocation.
     */
    public S getStoreBeforeMethodInvocation(MethodInvocationTree tree) {
        return storesBeforeMethodInvocation.get(getNodeForTree(tree));
    }
}
