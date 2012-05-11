package checkers.flow.analysis.checkers;

import java.util.List;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;

import checkers.flow.analysis.ConditionalTransferResult;
import checkers.flow.analysis.FlowExpressions;
import checkers.flow.analysis.FlowExpressions.Receiver;
import checkers.flow.analysis.RegularTransferResult;
import checkers.flow.analysis.TransferFunction;
import checkers.flow.analysis.TransferInput;
import checkers.flow.analysis.TransferResult;
import checkers.flow.cfg.UnderlyingAST;
import checkers.flow.cfg.UnderlyingAST.Kind;
import checkers.flow.cfg.node.AbstractNodeVisitor;
import checkers.flow.cfg.node.AssertNode;
import checkers.flow.cfg.node.AssignmentNode;
import checkers.flow.cfg.node.CaseNode;
import checkers.flow.cfg.node.ConditionalNotNode;
import checkers.flow.cfg.node.EqualToNode;
import checkers.flow.cfg.node.FieldAccessNode;
import checkers.flow.cfg.node.LocalVariableNode;
import checkers.flow.cfg.node.MethodInvocationNode;
import checkers.flow.cfg.node.Node;
import checkers.flow.cfg.node.NotEqualNode;
import checkers.flow.cfg.node.StringConcatenateAssignmentNode;
import checkers.flow.cfg.node.TernaryExpressionNode;
import checkers.flow.util.FlowExpressionParseUtil;
import checkers.flow.util.FlowExpressionParseUtil.FlowExpressionContext;
import checkers.flow.util.FlowExpressionParseUtil.FlowExpressionParseException;
import checkers.quals.EnsuresAnnotation;
import checkers.quals.EnsuresAnnotationIf;
import checkers.types.AnnotatedTypeFactory;
import checkers.types.AnnotatedTypeMirror;
import checkers.util.AnnotationUtils;
import checkers.util.TreeUtils;

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
    public S initialStore(UnderlyingAST underlyingAST, /* @Nullable */
            List<LocalVariableNode> parameters) {
        S info = analysis.createEmptyStore();

        if (underlyingAST.getKind() == Kind.METHOD) {
            for (LocalVariableNode p : parameters) {
                AnnotatedTypeMirror anno = analysis.getFactory()
                        .getAnnotatedType(p.getElement());
                info.initializeMethodParameter(p,
                        analysis.createAbstractValue(anno.getAnnotations()));
            }
        }

        return info;
    }

    /**
     * The default visitor returns the input information unchanged, or in the
     * case of conditional input information, merged.
     */
    @Override
    public TransferResult<V, S> visitNode(Node n, TransferInput<V, S> in) {
        V value = null;

        // TODO: handle implicit/explicit this and go to correct factory method
        Tree tree = n.getTree();
        if (tree != null) {
            if (TreeUtils.canHaveTypeAnnotation(tree)) {
                value = getValueFromFactory(tree);
            }
        }

        if (in.containsTwoStores()) {
            S thenStore = in.getThenStore();
            S elseStore = in.getElseStore();
            return new ConditionalTransferResult<>(value, thenStore, elseStore);
        } else {
            S info = in.getRegularStore();
            return new RegularTransferResult<>(value, info);
        }
    }

    @Override
    public TransferResult<V, S> visitFieldAccess(FieldAccessNode n,
            TransferInput<V, S> p) {
        S store = p.getRegularStore();
        V value = store.getValue(n);
        // look up value in factory, if necessary
        // TODO: handle cases, where this is not allowed (e.g. contructors in
        // non-null type systems)
        if (value == null) {
            value = getValueFromFactory(n.getTree());
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
     * The resulting abstract value is the merge of the 'then' and 'else'
     * branch.
     */
    @Override
    public TransferResult<V, S> visitTernaryExpression(TernaryExpressionNode n,
            TransferInput<V, S> p) {
        TransferResult<V, S> result = super.visitTernaryExpression(n, p);
        S store = result.getRegularStore();
        V thenValue = p.getValueOfSubNode(n.getThenOperand());
        V elseValue = p.getValueOfSubNode(n.getElseOperand());
        V resultValue = null;
        if (thenValue != null && elseValue != null) {
            resultValue = thenValue.leastUpperBound(elseValue);
        }
        return new RegularTransferResult<>(resultValue, store);
    }

    /**
     * Revert the role of the 'thenStore' and 'elseStore'.
     */
    @Override
    public TransferResult<V, S> visitConditionalNot(ConditionalNotNode n,
            TransferInput<V, S> p) {
        TransferResult<V, S> result = super.visitConditionalNot(n, p);
        S thenStore = result.getThenStore();
        S elseStore = result.getElseStore();
        return new ConditionalTransferResult<>(result.getResultValue(),
                elseStore, thenStore);
    }

    @Override
    public TransferResult<V, S> visitEqualTo(EqualToNode n,
            TransferInput<V, S> p) {
        TransferResult<V, S> res = super.visitEqualTo(n, p);

        Node leftN = n.getLeftOperand();
        Node rightN = n.getRightOperand();
        V leftV = p.getValueOfSubNode(leftN);
        V rightV = p.getValueOfSubNode(rightN);

        // if annotations differ, use the one that is more precise for both
        // sides (and add it to the store if possible)
        ConditionalTransferResult<V, S> a;
        a = strengthenAnnotationOfEqualTo(res, rightN, leftV, rightV, false);
        if (a != null) {
            return a;
        }
        a = strengthenAnnotationOfEqualTo(res, leftN, rightV, leftV, false);
        if (a != null) {
            return a;
        }

        return res;
    }

    @Override
    public TransferResult<V, S> visitNotEqual(NotEqualNode n,
            TransferInput<V, S> p) {
        TransferResult<V, S> res = super.visitNotEqual(n, p);

        Node leftN = n.getLeftOperand();
        Node rightN = n.getRightOperand();
        V leftV = p.getValueOfSubNode(leftN);
        V rightV = p.getValueOfSubNode(rightN);

        // if annotations differ, use the one that is more precise for both
        // sides (and add it to the store if possible)
        ConditionalTransferResult<V, S> a;
        a = strengthenAnnotationOfEqualTo(res, rightN, leftV, rightV, true);
        if (a != null) {
            return a;
        }
        a = strengthenAnnotationOfEqualTo(res, leftN, rightV, leftV, true);
        if (a != null) {
            return a;
        }

        return res;
    }

    /**
     * Refine the annotation of {@code secondNode} if the annotation
     * {@code secondValue} is less precise than {@code firstvalue}. This is
     * possible, if {@code secondNode} is an expression that is tracked by the
     * store (e.g., a local variable or a field).
     * 
     * @param res
     *            The previous result.
     * @param notEqualTo
     *            If true, indicates that the logic is flipped (i.e., the
     *            information is added to the {@code elseStore} instead of the
     *            {@code thenStore}) for a not-equal comparison.
     * @return The conditional transfer result (if information has been added),
     *         or {@code null}.
     */
    protected ConditionalTransferResult<V, S> strengthenAnnotationOfEqualTo(
            TransferResult<V, S> res, Node secondNode, V firstValue,
            V secondValue, boolean notEqualTo) {
        if (firstValue != null
                && (secondValue == null || firstValue.isSubtypeOf(secondValue))) {
            Receiver secondInternal = FlowExpressions
                    .internalReprOf(secondNode);
            if (!secondInternal.containsUnknown()) {
                S thenStore = res.getRegularStore();
                S elseStore = thenStore.copy();
                if (notEqualTo) {
                    elseStore.insertValue(secondInternal, firstValue);
                } else {
                    thenStore.insertValue(secondInternal, firstValue);
                }
                return new ConditionalTransferResult<>(res.getResultValue(),
                        thenStore, elseStore);
            }
        }
        return null;
    }

    @Override
    public TransferResult<V, S> visitAssignment(AssignmentNode n,
            TransferInput<V, S> in) {
        Node lhs = n.getTarget();
        Node rhs = n.getExpression();

        S info = in.getRegularStore();
        V rhsValue = in.getValueOfSubNode(rhs);
        processCommonAssignment(in, lhs, rhs, info, rhsValue);

        return new RegularTransferResult<>(rhsValue, info);
    }

    @Override
    public TransferResult<V, S> visitStringConcatenateAssignment(
            StringConcatenateAssignmentNode n, TransferInput<V, S> in) {
        TransferResult<V, S> result = super.visitStringConcatenateAssignment(n,
                in);
        Node lhs = n.getLeftOperand();
        Node rhs = n.getRightOperand();

        // update the results store if the assignment target is something we can
        // process
        S info = result.getRegularStore();
        V rhsValue = result.getResultValue();
        processCommonAssignment(in, lhs, rhs, info, rhsValue);

        return result;
    }

    /**
     * Determine abstract value of right-hand side and update the store
     * accordingly to the assignment.
     * 
     * @param rhsValue
     */
    protected void processCommonAssignment(TransferInput<V, S> in, Node lhs,
            Node rhs, S info, V rhsValue) {

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

        // assignment to array (not treated)
        else {
            info.updateForUnknownAssignment(lhs);
        }
    }

    @Override
    public TransferResult<V, S> visitMethodInvocation(MethodInvocationNode n,
            TransferInput<V, S> in) {

        S thenStore = in.getRegularStore();
        ExecutableElement method = n.getTarget().getMethod();

        V resValue = null;

        Tree tree = n.getTree();
        if (tree != null) {
            // use value from factory (no flowsensitive information available)
            resValue = getValueFromFactory(tree);
        }

        thenStore.updateForMethodCall(n, analysis.factory);

        // add new information based on postcondition
        AnnotationMirror ensuresAnnotation = analysis.factory
                .getDeclAnnotation(method, EnsuresAnnotation.class);
        if (ensuresAnnotation != null) {
            List<String> expressions = AnnotationUtils.elementValueArray(
                    ensuresAnnotation, "expression");
            String annotation = AnnotationUtils.elementValueClassName(
                    ensuresAnnotation, "annotation");
            AnnotationMirror anno = analysis.factory
                    .annotationFromName(annotation);

            FlowExpressionContext flowExprContext = FlowExpressionParseUtil
                    .buildFlowExprContextForUse(n);

            for (String exp : expressions) {
                FlowExpressions.Receiver r = null;
                try {
                    r = FlowExpressionParseUtil.parse(exp, flowExprContext);
                } catch (FlowExpressionParseException e) {
                    // these errors are reported at the declaration, ignore here
                }
                if (r != null) {
                    thenStore.insertValue(r, anno);
                }
            }
        }

        // add new information based on conditional postcondition
        AnnotationMirror ensuresAnnotationIf = analysis.factory
                .getDeclAnnotation(method, EnsuresAnnotationIf.class);
        if (ensuresAnnotationIf != null) {
            S elseStore = thenStore.copy();
            List<String> expressions = AnnotationUtils.elementValueArray(
                    ensuresAnnotationIf, "expression");
            String annotation = AnnotationUtils.elementValueClassName(
                    ensuresAnnotationIf, "annotation");
            boolean result = AnnotationUtils.elementValue(ensuresAnnotationIf,
                    "result", Boolean.class);
            AnnotationMirror anno = analysis.factory
                    .annotationFromName(annotation);

            FlowExpressionContext flowExprContext = FlowExpressionParseUtil
                    .buildFlowExprContextForUse(n);

            for (String exp : expressions) {
                FlowExpressions.Receiver r = null;
                try {
                    r = FlowExpressionParseUtil.parse(exp, flowExprContext);
                } catch (FlowExpressionParseException e) {
                    // these errors are reported at the declaration, ignore here
                }
                if (r != null) {
                    if (result) {
                        thenStore.insertValue(r, anno);
                    } else {
                        elseStore.insertValue(r, anno);
                    }
                }
            }

            return new ConditionalTransferResult<>(resValue, thenStore,
                    elseStore);
        }

        return new RegularTransferResult<>(resValue, thenStore);
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