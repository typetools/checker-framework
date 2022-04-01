package org.checkerframework.dataflow.cfg.builder;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.UnaryTree;

import org.checkerframework.dataflow.cfg.UnderlyingAST;
import org.checkerframework.dataflow.cfg.builder.ExtendedNode.ExtendedNodeType;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.ReturnNode;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

/* --------------------------------------------------------- */
/* Phase One */
/* --------------------------------------------------------- */

/** A wrapper object to pass around the result of phase one. */
public class PhaseOneResult {

    /** AST for which the CFG is to be built. */
    /*package-private*/ final UnderlyingAST underlyingAST;

    /**
     * Maps from AST {@link Tree}s to sets of {@link Node}s. Every Tree that produces a value will
     * have at least one corresponding Node. Trees that undergo conversions, such as boxing or
     * unboxing, can map to two distinct Nodes. The Node for the pre-conversion value is stored in
     * the treeLookupMap, while the Node for the post-conversion value is stored in the
     * convertedTreeLookupMap.
     */
    /*package-private*/ final IdentityHashMap<Tree, Set<Node>> treeLookupMap;

    /** Map from AST {@link Tree}s to post-conversion sets of {@link Node}s. */
    /*package-private*/ final IdentityHashMap<Tree, Set<Node>> convertedTreeLookupMap;

    /**
     * Map from postfix increment or decrement trees that are AST {@link UnaryTree}s to the
     * synthetic tree that is {@code v + 1} or {@code v - 1}.
     */
    /*package-private*/ final IdentityHashMap<UnaryTree, BinaryTree> postfixLookupMap;

    /** The list of extended nodes. */
    /*package-private*/ final ArrayList<ExtendedNode> nodeList;

    /** The bindings of labels to positions (i.e., indices) in the {@code nodeList}. */
    /*package-private*/ final Map<Label, Integer> bindings;

    /** The set of leaders (represented as indices into {@code nodeList}). */
    /*package-private*/ final Set<Integer> leaders;

    /**
     * All return nodes (if any) encountered. Only includes return statements that actually return
     * something.
     */
    /*package-private*/ final List<ReturnNode> returnNodes;

    /** Special label to identify the regular exit. */
    /*package-private*/ final Label regularExitLabel;

    /** Special label to identify the exceptional exit. */
    /*package-private*/ final Label exceptionalExitLabel;

    /**
     * Class declarations that have been encountered when building the control-flow graph for a
     * method.
     */
    /*package-private*/ final List<ClassTree> declaredClasses;

    /**
     * Lambdas encountered when building the control-flow graph for a method, variable initializer,
     * or initializer.
     */
    /*package-private*/ final List<LambdaExpressionTree> declaredLambdas;

    public PhaseOneResult(
            UnderlyingAST underlyingAST,
            IdentityHashMap<Tree, Set<Node>> treeLookupMap,
            IdentityHashMap<Tree, Set<Node>> convertedTreeLookupMap,
            IdentityHashMap<UnaryTree, BinaryTree> postfixLookupMap,
            ArrayList<ExtendedNode> nodeList,
            Map<Label, Integer> bindings,
            Set<Integer> leaders,
            List<ReturnNode> returnNodes,
            Label regularExitLabel,
            Label exceptionalExitLabel,
            List<ClassTree> declaredClasses,
            List<LambdaExpressionTree> declaredLambdas) {
        this.underlyingAST = underlyingAST;
        this.treeLookupMap = treeLookupMap;
        this.convertedTreeLookupMap = convertedTreeLookupMap;
        this.postfixLookupMap = postfixLookupMap;
        this.nodeList = nodeList;
        this.bindings = bindings;
        this.leaders = leaders;
        this.returnNodes = returnNodes;
        this.regularExitLabel = regularExitLabel;
        this.exceptionalExitLabel = exceptionalExitLabel;
        this.declaredClasses = declaredClasses;
        this.declaredLambdas = declaredLambdas;
    }

    @Override
    public String toString() {
        StringJoiner sj = new StringJoiner(System.lineSeparator());
        for (ExtendedNode n : nodeList) {
            sj.add(nodeToString(n));
        }
        return sj.toString();
    }

    protected String nodeToString(ExtendedNode n) {
        if (n.getType() == ExtendedNodeType.CONDITIONAL_JUMP) {
            ConditionalJump t = (ConditionalJump) n;
            return "TwoTargetConditionalJump("
                    + resolveLabel(t.getThenLabel())
                    + ", "
                    + resolveLabel(t.getElseLabel())
                    + ")";
        } else if (n.getType() == ExtendedNodeType.UNCONDITIONAL_JUMP) {
            return "UnconditionalJump(" + resolveLabel(n.getLabel()) + ")";
        } else {
            return n.toString();
        }
    }

    private String resolveLabel(Label label) {
        Integer index = bindings.get(label);
        if (index == null) {
            return "unbound label: " + label;
        }
        return nodeToString(nodeList.get(index));
    }

    /**
     * Returns a representation of a map, one entry per line.
     *
     * @param <K> the key type of the map
     * @param <V> the value type of the map
     * @param map a map
     * @return a representation of a map, one entry per line
     */
    private <K, V> String mapToString(Map<K, V> map) {
        if (map.isEmpty()) {
            return "{}";
        }
        StringJoiner result =
                new StringJoiner(
                        String.format("%n    "),
                        String.format("{%n    "),
                        String.format("%n    }"));
        for (Map.Entry<K, V> entry : map.entrySet()) {
            result.add(entry.getKey() + " => " + entry.getValue());
        }
        return result.toString();
    }

    /**
     * Returns a verbose string representation of this, useful for debugging.
     *
     * @return a string representation of this
     */
    public String toStringDebug() {
        StringJoiner result =
                new StringJoiner(
                        String.format("%n  "),
                        String.format("PhaseOneResult{%n  "),
                        String.format("%n  }"));
        result.add("treeLookupMap=" + mapToString(treeLookupMap));
        result.add("convertedTreeLookupMap=" + mapToString(convertedTreeLookupMap));
        result.add("postfixLookupMap=" + mapToString(postfixLookupMap));
        result.add("underlyingAST=" + underlyingAST);
        result.add("bindings=" + bindings);
        result.add("nodeList=" + CFGBuilder.extendedNodeCollectionToStringDebug(nodeList));
        result.add("leaders=" + leaders);
        result.add("returnNodes=" + Node.nodeCollectionToString(returnNodes));
        result.add("regularExitLabel=" + regularExitLabel);
        result.add("exceptionalExitLabel=" + exceptionalExitLabel);
        result.add("declaredClasses=" + declaredClasses);
        result.add("declaredLambdas=" + declaredLambdas);
        return result.toString();
    }
}
