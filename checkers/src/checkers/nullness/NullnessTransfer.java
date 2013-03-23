package checkers.nullness;

import java.util.List;

import javacutils.AnnotationUtils;
import javacutils.TreeUtils;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

import checkers.flow.analysis.checkers.CFAbstractStore;
import checkers.initialization.InitializationTransfer;
import checkers.initialization.quals.Committed;
import checkers.initialization.quals.NonRaw;
import checkers.nullness.quals.NonNull;
import checkers.nullness.quals.Nullable;
import checkers.nullness.quals.PolyNull;
import checkers.quals.PolyAll;
import checkers.types.AnnotatedTypeMirror;
import checkers.types.AnnotatedTypeMirror.AnnotatedExecutableType;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;

import dataflow.analysis.ConditionalTransferResult;
import dataflow.analysis.FlowExpressions;
import dataflow.analysis.FlowExpressions.Receiver;
import dataflow.analysis.RegularTransferResult;
import dataflow.analysis.TransferInput;
import dataflow.analysis.TransferResult;
import dataflow.cfg.node.ArrayAccessNode;
import dataflow.cfg.node.FieldAccessNode;
import dataflow.cfg.node.MethodAccessNode;
import dataflow.cfg.node.MethodInvocationNode;
import dataflow.cfg.node.Node;
import dataflow.cfg.node.NullLiteralNode;
import dataflow.cfg.node.ReturnNode;
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
 * <li>Tracks whether {@link PolyNull} is known to be {@link Nullable}.
 * </ol>
 *
 * @author Stefan Heule
 */
public class NullnessTransfer extends
        InitializationTransfer<NullnessValue, NullnessTransfer, NullnessStore> {

    /** Type-specific version of super.analysis. */
    protected final NullnessAnalysis analysis;

    /** Annotations of the non-null type system. */
    protected final AnnotationMirror NONNULL, NULLABLE;

    public NullnessTransfer(NullnessAnalysis analysis) {
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
    protected void makeNonNull(NullnessStore store, Node node) {
        Receiver internalRepr = FlowExpressions.internalReprOf(
                analysis.getFactory(), node);
        store.insertValue(internalRepr, NONNULL);
    }

    /**
     * Sets a given {@link Node} {@code node} to non-null in the given
     * {@link TransferResult}.
     */
    protected void makeNonNull(
            TransferResult<NullnessValue, NullnessStore> result, Node node) {
        if (result.containsTwoStores()) {
            makeNonNull(result.getThenStore(), node);
            makeNonNull(result.getElseStore(), node);
        } else {
            makeNonNull(result.getRegularStore(), node);
        }
    }

    @Override
    protected NullnessValue finishValue(NullnessValue value, NullnessStore store) {
        value = super.finishValue(value, store);
        if (value != null) {
            value.isPolyNullNull = store.isPolyNullNull();
        }
        return value;
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
    protected TransferResult<NullnessValue, NullnessStore> strengthenAnnotationOfEqualTo(
            TransferResult<NullnessValue, NullnessStore> res, Node firstNode,
            Node secondNode, NullnessValue firstValue, NullnessValue secondValue,
            boolean notEqualTo) {
        res = super.strengthenAnnotationOfEqualTo(res, firstNode, secondNode,
                firstValue, secondValue, notEqualTo);
        if (firstNode instanceof NullLiteralNode) {
            NullnessStore thenStore = res.getThenStore();
            NullnessStore elseStore = res.getElseStore();

            List<Node> secondParts = splitAssignments(secondNode);
            for (Node secondPart : secondParts) {
                Receiver secondInternal = FlowExpressions.internalReprOf(
                        analysis.getFactory(), secondPart);
                if (CFAbstractStore.canInsertReceiver(secondInternal)) {
                    thenStore = thenStore == null ? res.getThenStore()
                            : thenStore;
                    elseStore = elseStore == null ? res.getElseStore()
                            : elseStore;
                    if (notEqualTo) {
                        thenStore.insertValue(secondInternal, NONNULL);
                    } else {
                        elseStore.insertValue(secondInternal, NONNULL);
                    }
                }
            }

            if (secondValue != null
                    && (secondValue.getType().hasAnnotation(PolyNull.class) || secondValue
                            .getType().hasAnnotation(PolyAll.class))) {
                thenStore = thenStore == null ? res.getThenStore() : thenStore;
                elseStore = elseStore == null ? res.getElseStore() : elseStore;
                thenStore.setPolyNullNull(true);
            }

            if (thenStore != null) {
                return new ConditionalTransferResult<>(res.getResultValue(),
                        thenStore, elseStore);
            }
        }
        return res;
    }

    @Override
    public TransferResult<NullnessValue, NullnessStore> visitArrayAccess(
            ArrayAccessNode n, TransferInput<NullnessValue, NullnessStore> p) {
        TransferResult<NullnessValue, NullnessStore> result = super
                .visitArrayAccess(n, p);
        makeNonNull(result, n.getArray());
        return result;
    }

    @Override
    public TransferResult<NullnessValue, NullnessStore> visitMethodAccess(
            MethodAccessNode n, TransferInput<NullnessValue, NullnessStore> p) {
        TransferResult<NullnessValue, NullnessStore> result = super
                .visitMethodAccess(n, p);
        makeNonNull(result, n.getReceiver());
        return result;
    }

    @Override
    public TransferResult<NullnessValue, NullnessStore> visitFieldAccess(
            FieldAccessNode n, TransferInput<NullnessValue, NullnessStore> p) {
        TransferResult<NullnessValue, NullnessStore> result = super
                .visitFieldAccess(n, p);
        makeNonNull(result, n.getReceiver());
        return result;
    }

    @Override
    public TransferResult<NullnessValue, NullnessStore> visitThrow(ThrowNode n,
            TransferInput<NullnessValue, NullnessStore> p) {
        TransferResult<NullnessValue, NullnessStore> result = super.visitThrow(n,
                p);
        makeNonNull(result, n.getExpression());
        return result;
    }

    @Override
    public TransferResult<NullnessValue, NullnessStore> visitMethodInvocation(
            MethodInvocationNode n, TransferInput<NullnessValue, NullnessStore> in) {
        TransferResult<NullnessValue, NullnessStore> result = super
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
    public TransferResult<NullnessValue, NullnessStore> visitUnboxing(
            UnboxingNode n, TransferInput<NullnessValue, NullnessStore> p) {
        TransferResult<NullnessValue, NullnessStore> result = super
                .visitUnboxing(n, p);
        makeNonNull(result, n);
        return result;
    }

    @Override
    public TransferResult<NullnessValue, NullnessStore> visitReturn(ReturnNode n,
            TransferInput<NullnessValue, NullnessStore> in) {
        // HACK: make sure we have a value for return statements, because we
        // need to record whether (at this return statement) isPolyNullNull is
        // set or not.
        NullnessValue value = createDummyValue();
        if (in.containsTwoStores()) {
            NullnessStore thenStore = in.getThenStore();
            NullnessStore elseStore = in.getElseStore();
            return new ConditionalTransferResult<>(finishValue(value,
                    thenStore, elseStore), thenStore, elseStore);
        } else {
            NullnessStore info = in.getRegularStore();
            return new RegularTransferResult<>(finishValue(value, info), info);
        }
    }

    /**
     * Creates a dummy abstract value (whose type is not supposed to be looked at).
     */
    private NullnessValue createDummyValue() {
        TypeMirror dummy = analysis.getEnv().getTypeUtils()
                .getPrimitiveType(TypeKind.BOOLEAN);
        AnnotatedTypeMirror annotatedDummy = AnnotatedTypeMirror.createType(
                dummy, analysis.getFactory());
        annotatedDummy.addAnnotation(NonNull.class);
        annotatedDummy.addAnnotation(NonRaw.class);
        annotatedDummy.addAnnotation(Committed.class);
        NullnessValue value = new NullnessValue(analysis, annotatedDummy);
        return value;
    }
}
