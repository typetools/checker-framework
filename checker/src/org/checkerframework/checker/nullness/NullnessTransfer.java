package org.checkerframework.checker.nullness;

import org.checkerframework.checker.initialization.InitializationTransfer;
import org.checkerframework.checker.initialization.qual.Initialized;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.NonRaw;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.PolyNull;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.dataflow.analysis.ConditionalTransferResult;
import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.dataflow.analysis.FlowExpressions.Receiver;
import org.checkerframework.dataflow.analysis.RegularTransferResult;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.ArrayAccessNode;
import org.checkerframework.dataflow.cfg.node.FieldAccessNode;
import org.checkerframework.dataflow.cfg.node.MethodAccessNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.NullLiteralNode;
import org.checkerframework.dataflow.cfg.node.ReturnNode;
import org.checkerframework.dataflow.cfg.node.ThrowNode;
import org.checkerframework.framework.flow.CFAbstractStore;
import org.checkerframework.framework.qual.PolyAll;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.util.FlowExpressionParseUtil;
import org.checkerframework.framework.util.FlowExpressionParseUtil.FlowExpressionContext;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;

import java.util.List;
import java.util.Map;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

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

    protected final KeyForAnnotatedTypeFactory keyForTypeFactory;

    public NullnessTransfer(NullnessAnalysis analysis) {
        super(analysis);
        this.analysis = analysis;
        this.keyForTypeFactory = ((BaseTypeChecker)analysis.getTypeFactory().getContext().getChecker()).getTypeFactoryOfSubchecker(KeyForSubchecker.class);
        NONNULL = AnnotationUtils.fromClass(analysis.getTypeFactory()
                .getElementUtils(), NonNull.class);
        NULLABLE = AnnotationUtils.fromClass(analysis.getTypeFactory()
                .getElementUtils(), Nullable.class);
    }

    /**
     * Sets a given {@link Node} to non-null in the given {@code store}. Calls
     * to this method implement case 2.
     */
    protected void makeNonNull(NullnessStore store, Node node) {
        Receiver internalRepr = FlowExpressions.internalReprOf(
                analysis.getTypeFactory(), node);
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
                        analysis.getTypeFactory(), secondPart);
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

    /*
     * Provided that m is of a type that implements interface java.util.Map:
     * -Given a call m.get(k), if k is @KeyFor("m"), ensures that the result is @NonNull in the thenStore and elseStore of the transfer result.
     */
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
        AnnotatedExecutableType methodType = analysis.getTypeFactory()
                .getAnnotatedType(method);
        List<AnnotatedTypeMirror> methodParams = methodType.getParameterTypes();
        List<? extends ExpressionTree> methodArgs = tree.getArguments();
        for (int i = 0; i < methodParams.size() && i < methodArgs.size(); ++i) {
            if (methodParams.get(i).hasAnnotation(NONNULL)) {
                makeNonNull(result, n.getArgument(i));
            }
        }

        // Handle KeyFor annotations

        String methodName = n.getTarget().getMethod().toString();

        // First verify if the method name is get. This is an inexpensive check.

        if (methodName.startsWith("get(")) {
            // Now verify that the receiver of the method invocation is of a type
            // that extends that java.util.Map interface. This is a more expensive check.

            javax.lang.model.util.Types types = analysis.getTypes();

            TypeMirror mapInterfaceTypeMirror = types.erasure(TypesUtils.typeFromClass(types, analysis.getEnv().getElementUtils(), Map.class));

            TypeMirror receiverType = types.erasure(n.getTarget().getReceiver().getType());

            if (types.isSubtype(receiverType, mapInterfaceTypeMirror)) {

                FlowExpressionContext flowExprContext = FlowExpressionParseUtil
                        .buildFlowExprContextForUse(n, analysis.getTypeFactory().getContext());

                String mapName = flowExprContext.receiver.toString();
                AnnotationMirror am = keyForTypeFactory.createKeyForAnnotationMirrorWithValue(mapName); // @KeyFor(mapName)

                AnnotatedTypeMirror type = keyForTypeFactory.getAnnotatedType(methodArgs.get(0));

                if (type != null && keyForTypeFactory.keyForValuesSubtypeCheck(am, type, tree, n)) {
                    makeNonNull(result, n);

                    NullnessValue oldResultValue = result.getResultValue();
                    NullnessValue refinedResultValue = analysis.createSingleAnnotationValue(
                            NONNULL, oldResultValue.getType().getUnderlyingType());
                    NullnessValue newResultValue = refinedResultValue.mostSpecific(
                            oldResultValue, null);
                    result.setResultValue(newResultValue);
                }
            }
        }

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
                dummy, analysis.getTypeFactory(), false);
        annotatedDummy.addAnnotation(NonNull.class);
        annotatedDummy.addAnnotation(NonRaw.class);
        annotatedDummy.addAnnotation(Initialized.class);
        NullnessValue value = new NullnessValue(analysis, annotatedDummy);
        return value;
    }
}
