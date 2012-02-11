package checkers.flow.analysis.checkers;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;

import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;

import checkers.flow.analysis.AbstractValue;
import checkers.flow.analysis.Analysis;
import checkers.flow.analysis.ConditionalTransferResult;
import checkers.flow.analysis.RegularTransferResult;
import checkers.flow.analysis.Store;
import checkers.flow.analysis.TransferFunction;
import checkers.flow.analysis.TransferInput;
import checkers.flow.analysis.TransferResult;
import checkers.flow.cfg.CFGDOTVisualizer;
import checkers.flow.cfg.ControlFlowGraph;
import checkers.flow.cfg.node.AssignmentNode;
import checkers.flow.cfg.node.LocalVariableNode;
import checkers.flow.cfg.node.Node;
import checkers.flow.cfg.node.SinkNodeVisitor;
import checkers.flow.cfg.node.ValueLiteralNode;
import checkers.flow.cfg.node.VariableDeclarationNode;
import checkers.types.AnnotatedTypeFactory;
import checkers.types.AnnotatedTypeMirror;
import checkers.types.QualifierHierarchy;
import checkers.types.TreeAnnotationPropagator;
import checkers.types.TypeAnnotationProvider;
import checkers.util.InternalUtils;

/**
 * DefaultTypeAnalysis characterizes a kind of abstract value that is
 * computed by the checker framework's default flow-sensitive analysis.
 * It is parameterized by a QualifierHierarchy, defining the relationship
 * among type annotations and an AnnotatedTypeFactory, providing the
 * static type annotations for AST Trees.
 *
 * The inner class DefaultTypeAnalysis.Value represents a single
 * abstract value computed by the checker framework's default
 * flow-sensitive analysis.  A Value is a set of type annotations
 * from the QualifierHierarchy.
 * 
 * The inner class DefaultTypeAnalysis.NodeInfo represents the set
 * of dataflow facts known at a program point, which is a mapping from
 * values, represented by CFG Nodes, to sets of type annotations,
 * represented by DefaultTypeAnalysis.Values.
 *
 * TODO: Since statically known annotations provided by
 * the AnnotatedTypeFactory are upper bounds, avoid storing
 * NodeInfos explicitly unless they are more precise than the static
 * annotations.
 *
 * The inner class DefaultTypeAnalysis.Transfer is the transfer function
 * mapping input dataflow facts to output facts.  For the default analysis,
 * it merely tracks type annotations through assignments to local variables.
 * Improvements in the precision of type annotations arise from assignments
 * whose RHS has a more precise static type than their LHS.
 *
 * @author Charlie Garrett
 * 
 */
public class DefaultTypeAnalysis
    extends Analysis<DefaultTypeAnalysis.Value, DefaultTypeAnalysis.NodeInfo,
            DefaultTypeAnalysis.Transfer> {
    /**
     * The qualifier hierarchy for which to track annotations.
     */
    protected final QualifierHierarchy  typeHierarchy;

    /**
     * A type factory that can provide static type annotations
     * for AST Trees.
     */
    protected final AnnotatedTypeFactory factory;

    /**
     * The full set of annotations allowed for this type hierarchy.
     */
    protected final Set<AnnotationMirror> legalAnnotations;

    /**
     * A tree annotation propagator allows propagation of types
     * through AST trees.
     */
    protected final TreeAnnotationPropagator propagator;

    public DefaultTypeAnalysis(QualifierHierarchy typeHierarchy,
                               AnnotatedTypeFactory factory) {
        super(new Transfer());
        this.typeHierarchy = typeHierarchy;
        this.legalAnnotations = typeHierarchy.getAnnotations();
        this.factory = factory;
        this.propagator = factory.getAnnotationPropagator();
        this.transferFunction.setAnalysis(this);
    }


    /**
     * An abstact value for the default analysis is a set
     * of annotations from the QualifierHierarchy.
     */
    public class Value implements AbstractValue<Value> {
        private Set<AnnotationMirror> annotations;
        
        private Value() {
            annotations = new HashSet<AnnotationMirror>();
        }

        private Value(Set<AnnotationMirror> annotations) {
            this.annotations = annotations;
        }

        public Set<AnnotationMirror> getAnnotations() {
            return annotations;
        }

        /**
         * Computes and returns the least upper bound of two sets
         * of type annotations.  The return value is always of type
         * DefaultTypeAnalysis.Value.
         */
        @Override
        public Value leastUpperBound(Value other) {
            Set<AnnotationMirror> lub =
                typeHierarchy.leastUpperBound(annotations,
                                              other.annotations);
            return new Value(lub);
        }

        /**
         * Return whether this Value is a proper subtype of the
         * argument Value.
         */
        boolean isSubtypeOf(Value other) {
            return typeHierarchy.isSubtype(annotations,
                                           other.annotations);
        }
    }

    /**
     * Create a new dataflow value with no type annotations.
     */
    public Value createValue() {
        return new Value();
    }

    /**
     * Create a new dataflow value with the given type annotations,
     * which must belong to the QualifierHierarchy for which this
     * DefaultTypeAnalysis was created.
     */
    public Value createValue(Set<AnnotationMirror> annotations)
            throws IllegalArgumentException {
        for (AnnotationMirror anno : annotations) {
            if (!legalAnnotations.contains(anno)) {
                throw new IllegalArgumentException();
            }
        }
        return new Value(annotations);
    }

    /**
     * Returns the flow insensitive type annotations of a CFG Node, as
     * computed by the checker for the Tree corresponding to the Node.
     * Not all Nodes have corresponding Trees and the method returns
     * null when no type information is available.
     */
    private /*@Nullable*/ Value flowInsensitiveValue(Node n) {
        Tree tree = n.getTree();
        if (tree == null) {
            return null;
        }

        AnnotatedTypeMirror type = factory.getAnnotatedType(tree);
        return new Value(type.getAnnotations());
    }

    /**
     * A store for the default analysis is a mapping from Nodes
     * to Values.  If no Value is explicitly stored for
     * a Node, we fall back on the statically known annotations.
     *
     * Only Nodes representing mutable values, such as
     * VariableDeclarationNodes are tracked.  If we compute a more
     * precise type annotation for a variable than its static
     * annotation, then it is entered into the NodeInfo and stays
     * there.
     *
     * TODO: Extend NodeInfo to track class member fields in the
     * same way as variables.
     */
    public class NodeInfo implements Store<NodeInfo> {
        private Map<Node, Value> mutableInfo;

        private NodeInfo() {
            mutableInfo = new IdentityHashMap<Node, Value>();
        }

        private NodeInfo(Map<Node, Value> mutableInfo) {
            this.mutableInfo = mutableInfo;
        }

        /**
         * Returns the most precise dataflow information known
         * for the argument Node.  If dataflow analysis has
         * yielded more precise information, it is returned.
         * Otherwise, if static information is available, that
         * is returned.  Otherwise, empty information is returned.
         */
        public Value getInformation(Node n) {
            if (mutableInfo.containsKey(n)) {
                return mutableInfo.get(n);
            } else if (nodeInformation.containsKey(n)) {
                return nodeInformation.get(n);
            } else {
                Value flowInsensitive = flowInsensitiveValue(n);
                if (flowInsensitive != null) {
                    return flowInsensitive;
                }
            }
            return createValue();
        }

        /**
         * @return the argument Value or the flow insensitive
         *         Value, whichever is more precise
         */
        private Value flowInsensitiveUpperBound(Node n, Value val) {
            Value flowInsensitive = flowInsensitiveValue(n);
            if ((val.getAnnotations().isEmpty() &&
                 !flowInsensitive.getAnnotations().isEmpty()) ||
                flowInsensitive.isSubtypeOf(val)) {
                return flowInsensitive;
            } else {
                return val;
            }
        }

        /**
         * In the current version, we explicitly store either the flow
         * sensitive information passed as an argument, or the flow
         * insensitive information, whichever is more precise.
         *
         * TODO: Ensure that flow sensitive information is always at
         * least as precise as flow insensitive information, so that
         * we can avoid this overhead and even avoid the cost of
         * explicitly storing information that is no better than flow
         * insensitive.
         */
        public void setInformation(Node n, Value val) {
            val = flowInsensitiveUpperBound(n, val);
            if (n.hasResult()) {
                nodeInformation.put(n, val);
            } else if (n instanceof VariableDeclarationNode) {
                mutableInfo.put(n, val);
            }
        }

        public void mergeInformation(Node n, Value val) {
            Value updatedVal = flowInsensitiveUpperBound(n, val);
            if (n.hasResult()) {
                if (nodeInformation.containsKey(n)) {
                    updatedVal = nodeInformation.get(n).leastUpperBound(val);
                }
                nodeInformation.put(n, updatedVal);
            } else if (n instanceof VariableDeclarationNode) {
                if (mutableInfo.containsKey(n)) {
                    updatedVal = mutableInfo.get(n).leastUpperBound(val);
                }
                mutableInfo.put(n, updatedVal);
            }
        }

        @Override
        public NodeInfo copy() {
            return new NodeInfo(new IdentityHashMap<>(mutableInfo));
        }

        @Override
        public NodeInfo leastUpperBound(NodeInfo other) {
            NodeInfo newInfo = copy();

            for (Entry<Node, Value> e : other.mutableInfo.entrySet()) {
                newInfo.mergeInformation(e.getKey(), e.getValue());
            }

            return newInfo;
        }

        /**
         * Returns true iff this NodeInfo contains a superset of the
         * map entries of the argument NodeInfo.  Note that we test
         * the entry keys and values by Java equality, not by any subtype
         * relationship.  This method is used primarily to simplify the
         * equals predicate.
         */
        private boolean supersetOf(NodeInfo other) {
            for (Entry<Node, Value> e : other.mutableInfo.entrySet()) {
                Node key = e.getKey();
                if (!mutableInfo.containsKey(key) ||
                    !mutableInfo.get(key).equals(e.getValue())) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public boolean equals(Object o) {
            if (o != null && o instanceof NodeInfo) {
                NodeInfo other = (NodeInfo)o;
                return this.supersetOf(other) && other.supersetOf(this);
            } else {
                return false;
            }
        }

        @Override
        public String toString() {
            StringBuilder result = new StringBuilder("NodeInfo (\\n");
            for (Map.Entry<Node, Value> entry : mutableInfo.entrySet()) {
                result.append(entry.getKey() + "->" +
                              entry.getValue().getAnnotations() + "\\n");
            }
            result.append(")");
            return result.toString();
        }
    }


    /**
     * The default analysis transfer function propagates information
     * through assignments to local variables.
     */
    public static class Transfer
        extends SinkNodeVisitor<TransferResult<NodeInfo>,
                                TransferInput<NodeInfo>>
        implements TransferFunction<NodeInfo> {

        private /*@LazyNonNull*/ DefaultTypeAnalysis analysis;
        
        public void setAnalysis(DefaultTypeAnalysis analysis) {
            this.analysis = analysis;
        }

        /**
         * The initial store maps method formal parameters to
         * their currently most refined type.
         */
        @Override
        public NodeInfo initialStore(MethodTree tree,
                                     List<LocalVariableNode> parameters) {
            NodeInfo info = analysis.new NodeInfo();

            for (LocalVariableNode p : parameters) {
                Value flowInsensitive = analysis.flowInsensitiveValue(p);
                assert flowInsensitive != null :
                    "Missing initial type information for method parameter";
                info.mergeInformation(p, flowInsensitive);
            }

            return info;
        }

        // TODO: We could use an intermediate classes such as ExpressionNode
        // to refactor visitors.  Propagation is appropriate for all expressions.

        /**
         * The default visitor returns the input information unchanged, or
         * in the case of conditional input information, merged.
         */
        @Override
        public TransferResult<NodeInfo> visitNode(Node n,
                                                               TransferInput<NodeInfo> in) {
            // TODO: Perform type propagation separately with a thenStore and an elseStore.
            NodeInfo info = in.getRegularStore();

            if (n.hasResult()) {
                Tree tree = n.getTree();
                assert tree != null : "Node has a result, but no Tree";

                NodeInfoProvider provider = analysis.new NodeInfoProvider(info);
                Set<AnnotationMirror> annotations = analysis.propagator.visit(tree, provider);
                Value value = analysis.createValue(annotations);
                info.setInformation(n, value);
            }

            return new RegularTransferResult<NodeInfo>(info);
        }

        @Override
        public TransferResult<NodeInfo> visitValueLiteral(ValueLiteralNode n,
                                                                       TransferInput<NodeInfo> in) {
            // Literal values always have their flow insensitive type.
            NodeInfo info = in.getRegularStore();
            Value flowInsensitive = analysis.flowInsensitiveValue(n);
            assert flowInsensitive != null : "No flow insensitive information for node";
            info.setInformation(n, flowInsensitive);
            return new RegularTransferResult<NodeInfo>(info);
        }

        /**
         * Map local variable uses to their declarations and extract the most
         * precise information available for the declaration.
         */
        @Override
        public TransferResult<NodeInfo>
            visitLocalVariable(LocalVariableNode n, TransferInput<NodeInfo> in) {
            NodeInfo info = in.getRegularStore();

            VariableDeclarationNode decl = n.getDeclaration();
            if (decl != null) {
                info.setInformation(n, info.getInformation(decl));
            }

            return new RegularTransferResult<NodeInfo>(info);
        }

        /**
         * Propagate information from the assignment's RHS to a variable
         * on the LHS, if the RHS has more precise information available.
         */
        @Override
        public TransferResult<NodeInfo>
            visitAssignment(AssignmentNode n, TransferInput<NodeInfo> in) {
            Node lhs = n.getTarget();
            Node rhs = n.getExpression();

            NodeInfo info = in.getRegularStore();
            Value lhsValue = info.getInformation(lhs);
            Value rhsValue = info.getInformation(rhs);

            // Skip assignments to arrays or fields.
            if (lhs instanceof LocalVariableNode) {
                VariableDeclarationNode decl = ((LocalVariableNode)lhs).getDeclaration();
                assert decl != null;
                info.setInformation(decl, rhsValue);
            }

            // The AssignmentNode itself is a value with the same
            // type as the RHS.
            info.setInformation(n, rhsValue);

            return new RegularTransferResult<NodeInfo>(info);
        }
    }


    /**
     * Map AST Trees to type annotations based on a NodeInfo store.
     */
    public class NodeInfoProvider implements TypeAnnotationProvider {
        private NodeInfo info;

        public NodeInfoProvider(NodeInfo info) {
            this.info = info;
        }

        /**
         * Given an AST Tree in the current CFG, return the most
         * precise annotated type for the Node corresponding to
         * that Tree.  Throws IllegalArgumentException if the Tree
         * is not found in the current CFG.
         *
         * @param tree   an AST tree to be typed
         *
         * @return  the most precise annotated type of tree known
         */
        @Override
        public Set<AnnotationMirror> getAnnotations(Tree tree)
            throws IllegalArgumentException {
            Node node = cfg.getNodeCorrespondingToTree(tree);
            if (node == null) {
                throw new IllegalArgumentException();
            }
            Value abstractValue = info.getInformation(node);
            return abstractValue.getAnnotations();
        }
    }

    /**
     * Return a string description of the current type annotations for a value.
     */
    public String getInformationAsString(Node n) {
        if (nodeInformation.containsKey(n)) {
            return nodeInformation.get(n).getAnnotations() + ":FS";
        } else {
            Value value = flowInsensitiveValue(n);
            if (value != null) {
                return value.getAnnotations() + ":FI";
            }
            return "";
        }
    }

    /**
     * Print a DOT graph of the CFG and analysis info for inspection.
     */
    public void outputToDotFile(String outputFile) {
        String s = CFGDOTVisualizer.visualize(cfg.getEntryBlock(), this);

        try {
            FileWriter fstream = new FileWriter(outputFile);
            BufferedWriter out = new BufferedWriter(fstream);
            out.write(s);
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
