package checkers.nonnull;

import java.util.List;

import javacutils.AnnotationUtils;
import javacutils.TreeUtils;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;

import checkers.flow.analysis.checkers.CFAbstractStore;
import checkers.flow.analysis.checkers.CFValue;
import checkers.initialization.InitializationTransfer;
import checkers.nonnull.quals.NonNull;
import checkers.nonnull.quals.Nullable;
import checkers.nonnull.quals.PolyNull;
import checkers.types.AnnotatedTypeMirror;
import checkers.types.AnnotatedTypeMirror.AnnotatedExecutableType;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;

import dataflow.analysis.ConditionalTransferResult;
import dataflow.analysis.FlowExpressions;
import dataflow.analysis.FlowExpressions.Receiver;
import dataflow.analysis.TransferInput;
import dataflow.analysis.TransferResult;
import dataflow.cfg.node.ArrayAccessNode;
import dataflow.cfg.node.FieldAccessNode;
import dataflow.cfg.node.MethodAccessNode;
import dataflow.cfg.node.MethodInvocationNode;
import dataflow.cfg.node.Node;
import dataflow.cfg.node.NullLiteralNode;
import dataflow.cfg.node.ThrowNode;
import dataflow.cfg.node.UnboxingNode;

/**
 * Transfer function for the non-null type system. Performs the following
 * refinements:
 * <ol>
 * <li>After an expression is compared with the {@code null} literal, then that
 * expression can safely be considered {@link NonNull} if the result of the
 * comparison is false.
 * <li>If an expression is dereferenced, then it can safely be assumed to
 * non-null in the future. If it would not be, then the dereference would have
 * raised a {@link NullPointerException}.
 * </ol>
 *
 * @author Stefan Heule
 */
public class NonNullTransfer extends
        InitializationTransfer<NonNullTransfer, NonNullStore> {

    /** Type-specific version of super.analysis. */
    protected final NonNullAnalysis analysis;

    /** Annotations of the non-null type system. */
    protected final AnnotationMirror NONNULL, NULLABLE;

    public NonNullTransfer(NonNullAnalysis analysis) {
        super(analysis);
        this.analysis = analysis;
        NONNULL = AnnotationUtils.fromClass(analysis.getFactory()
                .getElementUtils(), NonNull.class);
        NULLABLE = AnnotationUtils.fromClass(analysis.getFactory()
                .getElementUtils(), Nullable.class);
    }

    /**
     * Sets a given {@link Node} to non-null in the given {@code store}. Calls
     * to this method implement case 2.
     */
    protected void makeNonNull(NonNullStore store, Node node) {
        Receiver internalRepr = FlowExpressions.internalReprOf(
                analysis.getFactory(), node);
        store.insertValue(internalRepr, NONNULL);
    }

    /**
     * Sets a given {@link Node} {@code node} to non-null in the given
     * {@link TransferResult}.
     */
    protected void makeNonNull(TransferResult<CFValue, NonNullStore> result,
            Node node) {
        if (result.containsTwoStores()) {
            makeNonNull(result.getThenStore(), node);
            makeNonNull(result.getElseStore(), node);
        } else {
            makeNonNull(result.getRegularStore(), node);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * Furthermore, this method refines the type to {@code NonNull} for the
     * appropriate branch if an expression is compared to the {@code null}
     * literal (listed as case 1 in the class description).
     */
    @Override
    protected TransferResult<CFValue, NonNullStore> strengthenAnnotationOfEqualTo(
            TransferResult<CFValue, NonNullStore> res, Node firstNode,
            Node secondNode, CFValue firstValue, CFValue secondValue,
            boolean notEqualTo) {
        res = super.strengthenAnnotationOfEqualTo(res, firstNode, secondNode,
                firstValue, secondValue, notEqualTo);
        if (firstNode instanceof NullLiteralNode) {
            NonNullStore thenStore = res.getThenStore();
            NonNullStore elseStore = res.getElseStore();

            List<Node> secondParts = splitAssignments(secondNode);
            for (Node secondPart : secondParts) {
                Receiver secondInternal = FlowExpressions.internalReprOf(
                        analysis.getFactory(), secondPart);
                if (CFAbstractStore.canInsertReceiver(secondInternal)) {
                    thenStore = thenStore==null ? res.getThenStore() : thenStore;
                    elseStore = elseStore==null ? res.getElseStore() : elseStore;
                    if (notEqualTo) {
                        thenStore.insertValue(secondInternal, NONNULL);
                    } else {
                        elseStore.insertValue(secondInternal, NONNULL);
                    }
                }
            }

            if (secondValue.getType().hasAnnotation(PolyNull.class)) {
                thenStore = thenStore==null ? res.getThenStore() : thenStore;
                elseStore = elseStore==null ? res.getElseStore() : elseStore;
                thenStore.setPolyNullNull(true);
            }

            if (thenStore != null) {
                return new ConditionalTransferResult<>(
                        res.getResultValue(), thenStore, elseStore);
            }
        }
        return res;
    }

    @Override
    public TransferResult<CFValue, NonNullStore> visitArrayAccess(
            ArrayAccessNode n, TransferInput<CFValue, NonNullStore> p) {
        TransferResult<CFValue, NonNullStore> result = super.visitArrayAccess(
                n, p);
        makeNonNull(result, n.getArray());
        return result;
    }

    @Override
    public TransferResult<CFValue, NonNullStore> visitMethodAccess(
            MethodAccessNode n, TransferInput<CFValue, NonNullStore> p) {
        TransferResult<CFValue, NonNullStore> result = super.visitMethodAccess(
                n, p);
        makeNonNull(result, n.getReceiver());
        return result;
    }

    @Override
    public TransferResult<CFValue, NonNullStore> visitFieldAccess(
            FieldAccessNode n, TransferInput<CFValue, NonNullStore> p) {
        TransferResult<CFValue, NonNullStore> result = super.visitFieldAccess(
                n, p);
        makeNonNull(result, n.getReceiver());
        return result;
    }

    @Override
    public TransferResult<CFValue, NonNullStore> visitThrow(ThrowNode n,
            TransferInput<CFValue, NonNullStore> p) {
        TransferResult<CFValue, NonNullStore> result = super.visitThrow(n, p);
        makeNonNull(result, n.getExpression());
        return result;
    }

    @Override
    public TransferResult<CFValue, NonNullStore> visitMethodInvocation(
            MethodInvocationNode n, TransferInput<CFValue, NonNullStore> in) {
        TransferResult<CFValue, NonNullStore> result = super
                .visitMethodInvocation(n, in);

        // Make receiver non-null.
        makeNonNull(result, n.getTarget().getReceiver());

        // For all formal parameters with a non-null annotation, make the actual
        // argument non-null.
        MethodInvocationTree tree = n.getTree();
        ExecutableElement method = TreeUtils.elementFromUse(tree);
        AnnotatedExecutableType methodType = analysis.getFactory()
                .getAnnotatedType(method);
        List<AnnotatedTypeMirror> methodParams = methodType.getParameterTypes();
        List<? extends ExpressionTree> methodArgs = tree.getArguments();
        for (int i = 0; i < methodParams.size() && i < methodArgs.size(); ++i) {
            if (methodParams.get(i).hasAnnotation(NONNULL)) {
                makeNonNull(result, n.getArgument(i));
            }
        }
        return result;
    }

    @Override
    public TransferResult<CFValue, NonNullStore> visitUnboxing(UnboxingNode n,
            TransferInput<CFValue, NonNullStore> p) {
        TransferResult<CFValue, NonNullStore> result = super
                .visitUnboxing(n, p);
        makeNonNull(result, n);
        return result;
    }
}
