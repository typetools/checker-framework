package checkers.flow.analysis;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Map.Entry;

import checkers.flow.cfg.node.Node;
import checkers.flow.cfg.node.NodeVisitor;

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
    final protected Map<Node, A> nodeValues;

    /** Map from AST {@link Tree}s to {@link Node}s. */
    final protected Map<Tree, Node> treeLookup;

    protected final NodeVisitor<TransferResult<A, S>, TransferInput<A, S>> transferFunction;

    /**
     * Initialize empty result.
     */
    public AnalysisResult() {
        nodeValues = new IdentityHashMap<>();
        treeLookup = new IdentityHashMap<>();
        transferFunction = null;
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
     * Call the transfer function for node {@code node}, and set that node as
     * current node first.
     */
    protected TransferResult<A, S> callTransferFunction(Node node,
            TransferInput<A, S> store) {
        if (node.isLValue()) {
            // TODO: should the default behavior be to return either a regular
            // transfer result or a conditional transfer result (depending on
            // store.hasTwoStores()), or is the following correct?
            return new RegularTransferResult<A, S>(null,
                    store.getRegularStore());
        }
        store.node = node;
        TransferResult<A, S> transferResult = node.accept(transferFunction,
                store);
        return transferResult;
    }

}
