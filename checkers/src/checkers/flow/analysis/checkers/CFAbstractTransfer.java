package checkers.flow.analysis.checkers;

import java.util.List;

import checkers.flow.analysis.RegularTransferResult;
import checkers.flow.analysis.TransferFunction;
import checkers.flow.analysis.TransferInput;
import checkers.flow.analysis.TransferResult;
import checkers.flow.cfg.node.AbstractNodeVisitor;
import checkers.flow.cfg.node.AssertNode;
import checkers.flow.cfg.node.AssignmentNode;
import checkers.flow.cfg.node.CaseNode;
import checkers.flow.cfg.node.FieldAccessNode;
import checkers.flow.cfg.node.LocalVariableNode;
import checkers.flow.cfg.node.Node;
import checkers.types.AnnotatedTypeFactory;
import checkers.types.AnnotatedTypeMirror;
import checkers.util.TreeUtils;

import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;

/**
 * The default analysis transfer function for the Checker Framework propagates
 * information through assignments and uses the {@link AnnotatedTypeFactory} to
 * provide checker-specific logic how to combine types (e.g., what is the type
 * of a string concatenation, given the types of the two operands) and as an
 * abstraction function (e.g., determine the annotations on literals)..
 * 
 * @author Charlie Garrett
 * @author Stefan Heule
 */
public abstract class CFAbstractTransfer<V extends CFAbstractValue<V>, S extends CFAbstractStore<V, S>, T extends CFAbstractTransfer<V, S, T>>
        extends AbstractNodeVisitor<TransferResult<V, S>, TransferInput<V, S>>
        implements TransferFunction<V, S> {

    /**
     * The analysis class this store belongs to.
     */
    protected CFAbstractAnalysis<V, S, T> analysis;

    public CFAbstractTransfer(CFAbstractAnalysis<V, S, T> analysis) {
        this.analysis = analysis;
    }

    /**
     * @return The abstract value of a non-leaf tree {@code tree}, as computed
     *         by the {@link AnnotatedTypeFactory}.
     */
    protected V getValueFromFactory(Tree tree) {
        analysis.setCurrentTree(tree);
        AnnotatedTypeMirror at = analysis.factory.getAnnotatedType(tree);
        analysis.setCurrentTree(null);
        return analysis.createAbstractValue(at.getAnnotations());
    }

    /**
     * The initial store maps method formal parameters to their currently most
     * refined type.
     */
    @Override
    public S initialStore(MethodTree tree, List<LocalVariableNode> parameters) {
        S info = analysis.createEmptyStore();

        for (LocalVariableNode p : parameters) {
            V flowInsensitive = null; // TODO
            // assert flowInsensitive != null :
            // "Missing initial type information for method parameter";
            // info.mergeValue(p, flowInsensitive);
        }

        return info;
    }

    /**
     * The default visitor returns the input information unchanged, or in the
     * case of conditional input information, merged.
     */
    @Override
    public TransferResult<V, S> visitNode(Node n, TransferInput<V, S> in) {
        // TODO: Perform type propagation separately with a thenStore and an
        // elseStore.

        S info = in.getRegularStore();
        V value = null;

        // TODO: handle implicit/explicit this and go to correct factory method
        Tree tree = n.getTree();
        if (tree != null) {
            if (TreeUtils.canHaveTypeAnnotation(tree)) {
                value = getValueFromFactory(tree);
            }
        }

        return new RegularTransferResult<>(value, info);
    }

    @Override
    public TransferResult<V, S> visitFieldAccess(FieldAccessNode n,
            TransferInput<V, S> p) {
        S store = p.getRegularStore();
        V value = store.getValue(n);
        if (value == null) {
            Tree tree = n.getTree();
            assert tree != null;
            value = getValueFromFactory(tree);
        }
        return new RegularTransferResult<>(value, store);
    }

    /**
     * Use the most specific type information available according to the store.
     */
    @Override
    public TransferResult<V, S> visitLocalVariable(LocalVariableNode n,
            TransferInput<V, S> in) {
        S store = in.getRegularStore();
        V value = store.getValue(n);
        return new RegularTransferResult<>(value, store);
    }

    /**
     * Determine abstract value of right-hand side and update the store
     * accordingly to the assignment.
     */
    @Override
    public TransferResult<V, S> visitAssignment(AssignmentNode n,
            TransferInput<V, S> in) {
        Node lhs = n.getTarget();
        Node rhs = n.getExpression();

        S info = in.getRegularStore();
        V rhsValue = in.getValueOfSubNode(rhs);

        // assignment to a local variable
        if (lhs instanceof LocalVariableNode) {
            LocalVariableNode var = (LocalVariableNode) lhs;
            info.updateForAssignment(var, rhsValue);
        }

        // assignment to a field
        else if (lhs instanceof FieldAccessNode) {
            FieldAccessNode fieldAccess = (FieldAccessNode) lhs;
            info.updateForAssignment(fieldAccess, rhsValue);
        }

        else {
            info.updateForUnknownAssignment(lhs);
        }

        // TODO: other assignments

        return new RegularTransferResult<>(rhsValue, info);
    }

    /**
     * An assert produces no value and since it may be disabled, it has no
     * effect on the store.
     */
    @Override
    public TransferResult<V, S> visitAssert(AssertNode n, TransferInput<V, S> in) {
        // TODO: Perform type propagation separately with a thenStore and an
        // elseStore.
        return new RegularTransferResult<>(null, in.getRegularStore());
    }

    /**
     * A case produces no value, but it may imply some facts about the argument
     * to the switch statement.
     */
    @Override
    public TransferResult<V, S> visitCase(CaseNode n, TransferInput<V, S> in) {
        return new RegularTransferResult<>(null, in.getRegularStore());
    }
}