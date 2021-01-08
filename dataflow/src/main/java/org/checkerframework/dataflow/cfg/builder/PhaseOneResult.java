package org.checkerframework.dataflow.cfg.builder;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.UnaryTree;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import org.checkerframework.dataflow.cfg.UnderlyingAST;
import org.checkerframework.dataflow.cfg.builder.ExtendedNode.ExtendedNodeType;
import org.checkerframework.dataflow.cfg.node.AssignmentNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.ReturnNode;

/* --------------------------------------------------------- */
/* Phase One */
/* --------------------------------------------------------- */

/**
 * A wrapper object to pass around the result of phase one. For a documentation of the fields see
 * {@link CFGTranslationPhaseOne}.
 */
public class PhaseOneResult {

    final IdentityHashMap<Tree, Set<Node>> treeLookupMap;
    final IdentityHashMap<Tree, Set<Node>> convertedTreeLookupMap;
    final IdentityHashMap<UnaryTree, AssignmentNode> unaryAssignNodeLookupMap;
    final UnderlyingAST underlyingAST;
    final Map<Label, Integer> bindings;
    final ArrayList<ExtendedNode> nodeList;
    final Set<Integer> leaders;
    final List<ReturnNode> returnNodes;
    final Label regularExitLabel;
    final Label exceptionalExitLabel;
    final List<ClassTree> declaredClasses;
    final List<LambdaExpressionTree> declaredLambdas;

    public PhaseOneResult(
            UnderlyingAST underlyingAST,
            IdentityHashMap<Tree, Set<Node>> treeLookupMap,
            IdentityHashMap<Tree, Set<Node>> convertedTreeLookupMap,
            IdentityHashMap<UnaryTree, AssignmentNode> unaryAssignNodeLookupMap,
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
        this.unaryAssignNodeLookupMap = unaryAssignNodeLookupMap;
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
        result.add("treeLookupMap=" + treeLookupMap);
        result.add("convertedTreeLookupMap=" + convertedTreeLookupMap);
        result.add("unaryAssignNodeLookupMap=" + unaryAssignNodeLookupMap);
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
