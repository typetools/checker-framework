package checkers.flow.analysis.checkers;

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
import checkers.flow.analysis.ConditionalTransferResult;
import checkers.flow.analysis.RegularTransferResult;
import checkers.flow.analysis.Store;
import checkers.flow.analysis.TransferFunction;
import checkers.flow.analysis.TransferInput;
import checkers.flow.analysis.TransferResult;
import checkers.flow.cfg.node.AssignmentNode;
import checkers.flow.cfg.node.LocalVariableNode;
import checkers.flow.cfg.node.Node;
import checkers.flow.cfg.node.SinkNodeVisitor;
import checkers.flow.cfg.node.VariableDeclarationNode;
import checkers.types.AnnotatedTypeFactory;
import checkers.types.AnnotatedTypeMirror;
import checkers.types.QualifierHierarchy;
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
 * represented by DefaultTypeAnalysis.Values.  Since statically known
 * annotations provided by the AnnotatedTypeFactory are upper bounds,
 * we avoid storing NodeInfos explicitly unless they are more precise
 * than the static annotations.
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
public class DefaultTypeAnalysis {
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

    public DefaultTypeAnalysis(QualifierHierarchy typeHierarchy,
                               AnnotatedTypeFactory factory) {
        this.typeHierarchy = typeHierarchy;
        this.legalAnnotations = typeHierarchy.getAnnotations();
        this.factory = factory;
    }


    /**
     * An abstact value for the default analysis is a set
     * of annotations from the QualifierHierarchy.
     */
    public class Value implements AbstractValue<Value> {
        private Set<AnnotationMirror> annotatedTypes;
        
        private Value() {
            annotatedTypes = new HashSet<AnnotationMirror>();
        }

        private Value(Set<AnnotationMirror> annotatedTypes) {
            this.annotatedTypes = annotatedTypes;
        }

        /**
         * Computes and returns the least upper bound of two sets
         * of type annotations.  The return value is always of type
         * DefaultTypeAnalysis.Value.
         */
        @Override
        public /*@NonNull*/ Value leastUpperBound(Value other) {
            Set<AnnotationMirror> lub =
                typeHierarchy.leastUpperBound(annotatedTypes,
                                              other.annotatedTypes);
            return new Value(lub);
        }

        /**
         * Return whether this Value is a proper supertype of the
         * argument Value.
         */
        boolean isSupertypeOf(Value other) {
            // QualifierHierarchy's isSubtype method is only true for
            // proper subtypes.
            return typeHierarchy.isSubtype(annotatedTypes,
                                           other.annotatedTypes);
        }
    }

    /**
     * Create a new dataflow value with no type annotations.
     */
    public /*@NonNull*/ Value createValue() {
        return new Value();
    }

    /**
     * Create a new dataflow value with the given type annotations,
     * which must belong to the QualifierHierarchy for which this
     * DefaultTypeAnalysis was created.
     */
    public /*@NonNull*/ Value createValue(Set<AnnotationMirror> annotations)
            throws IllegalArgumentException {
        for (AnnotationMirror anno : annotations) {
            if (!legalAnnotations.contains(anno)) {
                throw new IllegalArgumentException();
            }
        }
        return new Value(annotations);
    }

    /**
     * Returns the flow insensitive type annotations of a CFG
     * Node, as computed by the checker and stored with the
     * language model Element corresponding to the Tree
     * corresponding to the Node.  Not all Nodes have
     * corresponding Elements and the method returns null when no
     * type information is available.
     */
    private /*@Nullable*/ Value flowInsensitiveValue(Node n) {
        Tree tree = n.getTree();
        if (tree == null) {
            return null;
        }

        Element element = InternalUtils.symbol(tree);
        if (element == null) {
            return null;
        }
            
        AnnotatedTypeMirror type = factory.getAnnotatedType(element);
        return new Value(type.getAnnotations());
    }

    
    /**
     * A store for the default analysis is a mapping from Nodes
     * to Values.  If no Value is explicitly stored for
     * a Node, we fall back on the statically known annotations.
     *
     * Two kinds of Nodes are tracked.  VariableDeclarationNodes
     * represent mutable variables.  If we compute a more precise
     * type annotation for a variable than its static annotation,
     * then it is entered into the NodeInfo and stays there.
     *
     * Value producing Nodes represent immutable values computed by
     * expressions.  Since we are processing a CFG that was originally
     * an AST and we do no transformation, we know that expressions
     * do not outlive their AST parents.  So even if we compute a
     * more precise type annotation for a variable that its static
     * annotation, we only store that information from the point where
     * it becomes true to the point where the the value becomes dead
     * (i.e. its AST parent).
     *
     * TODO: Extend NodeInfo to track class member fields like variables.
     */
    public class NodeInfo implements Store<NodeInfo> {
        private Map<Node, Value> info;

        private NodeInfo() {
            info = new IdentityHashMap<Node, Value>();
        }

        private NodeInfo(Map<Node, Value> info) {
            this.info = info;
        }

        /**
         * Returns the most precise dataflow information known
         * for the argument Node.  If dataflow analysis has
         * yielded more precise information, it is returned.
         * Otherwise, if static information is available, that
         * is returned.  Otherwise, empty information is returned.
         */
        public /*@NonNull*/ Value getInformation(Node decl) {
            if (info.containsKey(decl)) {
                return info.get(decl);
            } else {
                Value flowInsensitive = flowInsensitiveValue(decl);
                if (flowInsensitive != null) {
                    return flowInsensitive;
                }
            }
            return createValue();
        }

        public void setInformation(Node decl, Value val) {
            info.put(decl, val);
        }

        public void mergeInformation(Node decl, Value val) {
            Value updatedVal;
            if (info.containsKey(decl)) {
                updatedVal = info.get(decl).leastUpperBound(val);
            } else {
                updatedVal = val;
            }
            info.put(decl, updatedVal);
        }

        public void removeInformation(Node decl) {
            info.remove(decl);
        }

        @Override
        public /*@NonNull*/ NodeInfo copy() {
            return new NodeInfo(new IdentityHashMap<>(info));
        }

        @Override
        public /*@NonNull*/ NodeInfo leastUpperBound(NodeInfo other) {
            NodeInfo newInfo = copy();

            for (Entry<Node, Value> e : other.info.entrySet()) {
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
            for (Entry<Node, Value> e : other.info.entrySet()) {
                Node key = e.getKey();
                if (!info.containsKey(key) ||
                    !info.get(key).equals(e.getValue())) {
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
    }


    /**
     * The default analysis transfer function propagates information
     * through assignments to local variables.  It avoids explicitly
     * storing a Value and copying a NodeInfo unless the outflowing
     * information is more precise than the inflowing.
     */
    public class Transfer
        extends SinkNodeVisitor<TransferResult<NodeInfo>,
                                TransferInput<NodeInfo>>
        implements TransferFunction<NodeInfo> {

        /**
         * The initial store maps method formal parameters to
         * their currently most refined type.
         */
        @Override
        public /*@NonNull*/ NodeInfo initialStore(MethodTree tree,
                                     List<LocalVariableNode> parameters) {
            NodeInfo info = new NodeInfo();

            for (LocalVariableNode p : parameters) {
                Value flowInsensitive = flowInsensitiveValue(p);
                assert flowInsensitive != null :
                    "Missing initial type information for method parameter";
                info.mergeInformation(p, flowInsensitive);
            }

            return info;
        }

        /**
         * The default visitor returns the input information unchanged, or
         * in the case of conditional input information, merged.
         */
        @Override
        public /*@NonNull*/ TransferResult<NodeInfo> visitNode(Node n,
                                                               TransferInput<NodeInfo> in) {
            // TODO: Perform type propagation separately with a thenStore and an elseStore.
            NodeInfo info = in.getRegularStore().copy();

            // The node should not be present in the NodeInfo at this point.
            // So flow insensitive information is the best we have and we can
            // set rather than merge the flow-sensitive information if it is
            // more precise.
            Value flowInsensitive = flowInsensitiveValue(n);

            // TODO: Propagate types from operands to Node.
            // Set<AnnotationMirror> nodeType = n.propagate(info);
            // if (nodeType != null) {
            //     Value value = createValue(nodeType);
            //     if (flowInsensitive.isSupertypeOf(value)) {
            //         info.setInformation(n, value);
            //     }
            // }

            // Remove information about the operand Nodes because they are dead
            // after this Node (their AST parent).
            for (Node operand : n.getOperands()) {
                info.removeInformation(operand);
            }

            return new RegularTransferResult<NodeInfo>(info);
        }

        /**
         * Map local variable uses to their declarations and extract the most
         * precise information available for the declaration.
         */
        @Override
        public /*@NonNull*/ TransferResult<NodeInfo>
            visitLocalVariable(LocalVariableNode n, TransferInput<NodeInfo> in) {
            NodeInfo info = in.getRegularStore();

            VariableDeclarationNode decl = n.getDeclaration();
            if (decl != null) {
                info = info.copy();
                info.setInformation(n, info.getInformation(decl));
            }

            return new RegularTransferResult<NodeInfo>(info);
        }

        /**
         * Propagate information from the assignment's RHS to a variable
         * on the LHS, if the RHS has more precise information available.
         */
        @Override
        public /*@NonNull*/ TransferResult<NodeInfo>
            visitAssignment(AssignmentNode n, TransferInput<NodeInfo> in) {
            Node lhs = n.getTarget();
            Node rhs = n.getExpression();

            NodeInfo info = in.getRegularStore();
            Value lhsValue = info.getInformation(lhs);
            Value rhsValue = info.getInformation(rhs);

            // Skip assignments to arrays or fields.
            if (lhs instanceof LocalVariableNode) {
                if (lhsValue.isSupertypeOf(rhsValue)) {
                    info = info.copy();
                    info.setInformation(lhs, rhsValue);
                }
            }

            // The AssignmentNode itself is a value with the same
            // type as the RHS.
            Value assignValue = info.getInformation(n);
            if (assignValue.isSupertypeOf(rhsValue)) {
                info = info.copy();
                info.setInformation(n, rhsValue);
            }

            return new RegularTransferResult<NodeInfo>(info);
        }
    }
}
