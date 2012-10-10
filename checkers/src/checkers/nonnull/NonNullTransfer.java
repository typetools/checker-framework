package checkers.nonnull;

import java.util.List;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;

import javacutils.AnnotationUtils;
import javacutils.TreeUtils;

import checkers.flow.analysis.ConditionalTransferResult;
import checkers.flow.analysis.FlowExpressions;
import checkers.flow.analysis.FlowExpressions.Receiver;
import checkers.flow.analysis.TransferInput;
import checkers.flow.analysis.TransferResult;
import checkers.flow.analysis.checkers.CFAbstractStore;
import checkers.flow.analysis.checkers.CFValue;
import checkers.flow.cfg.node.ArrayAccessNode;
import checkers.flow.cfg.node.FieldAccessNode;
import checkers.flow.cfg.node.MethodAccessNode;
import checkers.flow.cfg.node.MethodInvocationNode;
import checkers.flow.cfg.node.Node;
import checkers.flow.cfg.node.NullLiteralNode;
import checkers.flow.cfg.node.ThrowNode;
import checkers.initialization.InitializationStore;
import checkers.initialization.InitializationTransfer;
import checkers.nonnull.quals.NonNull;
import checkers.types.AnnotatedTypeMirror;
import checkers.types.AnnotatedTypeMirror.AnnotatedExecutableType;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;

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
public class NonNullTransfer extends InitializationTransfer<NonNullTransfer> {

    /** Type-specific version of super.analysis. */
    protected final NonNullAnalysis analysis;

    /** Annotations of the non-null type system. */
    protected final AnnotationMirror NONNULL;

    public NonNullTransfer(NonNullAnalysis analysis) {
        super(analysis);
        this.analysis = analysis;
        NONNULL = AnnotationUtils.fromClass(analysis.getFactory().getElementUtils(), NonNull.class);
    }

    /**
     * Sets a given {@link Node} to non-null in the given {@code store}. Calls
     * to this method implement case 2.
     */
    protected void makeNonNull(InitializationStore store, Node node) {
        Receiver internalRepr = FlowExpressions.internalReprOf(
                analysis.getFactory(), node);
        store.insertValue(internalRepr, NONNULL);
    }

    /**
     * Sets a given {@link Node} {@code node} to non-null in the given
     * {@link TransferResult}.
     */
    protected void makeNonNull(TransferResult<CFValue, InitializationStore> result,
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
    protected TransferResult<CFValue, InitializationStore> strengthenAnnotationOfEqualTo(
            TransferResult<CFValue, InitializationStore> res, Node firstNode,
            Node secondNode, CFValue firstValue, CFValue secondValue,
            boolean notEqualTo) {
        res = super.strengthenAnnotationOfEqualTo(res, firstNode, secondNode,
                firstValue, secondValue, notEqualTo);
        // Case 1:
        if (firstNode instanceof NullLiteralNode) {
            Receiver secondInternal = FlowExpressions.internalReprOf(
                    analysis.getFactory(), secondNode);
            if (CFAbstractStore.canInsertReceiver(secondInternal)) {
                InitializationStore thenStore = res.getThenStore();
                InitializationStore elseStore = res.getElseStore();
                if (notEqualTo) {
                    thenStore.insertValue(secondInternal, NONNULL);
                } else {
                    elseStore.insertValue(secondInternal, NONNULL);
                }
                return new ConditionalTransferResult<>(res.getResultValue(),
                        thenStore, elseStore);
            }
        }
        return res;
    }

    @Override
    public TransferResult<CFValue, InitializationStore> visitArrayAccess(
            ArrayAccessNode n, TransferInput<CFValue, InitializationStore> p) {
        TransferResult<CFValue, InitializationStore> result = super
                .visitArrayAccess(n, p);
        makeNonNull(result, n.getArray());
        return result;
    }

    @Override
    public TransferResult<CFValue, InitializationStore> visitMethodAccess(
            MethodAccessNode n, TransferInput<CFValue, InitializationStore> p) {
        TransferResult<CFValue, InitializationStore> result = super
                .visitMethodAccess(n, p);
        makeNonNull(result, n.getReceiver());
        return result;
    }

    @Override
    public TransferResult<CFValue, InitializationStore> visitFieldAccess(
            FieldAccessNode n, TransferInput<CFValue, InitializationStore> p) {
        TransferResult<CFValue, InitializationStore> result = super
                .visitFieldAccess(n, p);
        makeNonNull(result, n.getReceiver());
        return result;
    }

    @Override
    public TransferResult<CFValue, InitializationStore> visitThrow(ThrowNode n,
            TransferInput<CFValue, InitializationStore> p) {
        TransferResult<CFValue, InitializationStore> result = super
                .visitThrow(n, p);
        makeNonNull(result, n.getExpression());
        return result;
    }

    @Override
    public TransferResult<CFValue, InitializationStore> visitMethodInvocation(
            MethodInvocationNode n, TransferInput<CFValue, InitializationStore> in) {
        TransferResult<CFValue, InitializationStore> result = super
                .visitMethodInvocation(n, in);
        // Make receiver non-null.
        makeNonNull(result, n.getTarget());

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
}
