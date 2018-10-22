package org.checkerframework.checker.nullness;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.initialization.InitializationTransfer;
import org.checkerframework.checker.nullness.qual.NonNull;
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
import org.checkerframework.dataflow.cfg.node.InstanceOfNode;
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
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TreeUtils;

/**
 * Transfer function for the non-null type system. Performs the following refinements:
 *
 * <ol>
 *   <li>After an expression is compared with the {@code null} literal, then that expression can
 *       safely be considered {@link NonNull} if the result of the comparison is false.
 *   <li>If an expression is dereferenced, then it can safely be assumed to non-null in the future.
 *       If it would not be, then the dereference would have raised a {@link NullPointerException}.
 *   <li>Tracks whether {@link PolyNull} is known to be {@link Nullable}.
 * </ol>
 */
public class NullnessTransfer
        extends InitializationTransfer<NullnessValue, NullnessTransfer, NullnessStore> {

    /** Annotations of the non-null type system. */
    protected final AnnotationMirror NONNULL, NULLABLE;

    protected final KeyForAnnotatedTypeFactory keyForTypeFactory;

    public NullnessTransfer(NullnessAnalysis analysis) {
        super(analysis);
        this.keyForTypeFactory =
                ((BaseTypeChecker) analysis.getTypeFactory().getContext().getChecker())
                        .getTypeFactoryOfSubchecker(KeyForSubchecker.class);
        NONNULL =
                AnnotationBuilder.fromClass(
                        analysis.getTypeFactory().getElementUtils(), NonNull.class);
        NULLABLE =
                AnnotationBuilder.fromClass(
                        analysis.getTypeFactory().getElementUtils(), Nullable.class);
    }

    /**
     * Sets a given {@link Node} to non-null in the given {@code store}. Calls to this method
     * implement case 2.
     */
    protected void makeNonNull(NullnessStore store, Node node) {
        Receiver internalRepr = FlowExpressions.internalReprOf(analysis.getTypeFactory(), node);
        store.insertValue(internalRepr, NONNULL);
    }

    /** Sets a given {@link Node} {@code node} to non-null in the given {@link TransferResult}. */
    protected void makeNonNull(TransferResult<NullnessValue, NullnessStore> result, Node node) {
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
     * <p>Furthermore, this method refines the type to {@code NonNull} for the appropriate branch if
     * an expression is compared to the {@code null} literal (listed as case 1 in the class
     * description).
     */
    @Override
    protected TransferResult<NullnessValue, NullnessStore> strengthenAnnotationOfEqualTo(
            TransferResult<NullnessValue, NullnessStore> res,
            Node firstNode,
            Node secondNode,
            NullnessValue firstValue,
            NullnessValue secondValue,
            boolean notEqualTo) {
        res =
                super.strengthenAnnotationOfEqualTo(
                        res, firstNode, secondNode, firstValue, secondValue, notEqualTo);
        if (firstNode instanceof NullLiteralNode) {
            NullnessStore thenStore = res.getThenStore();
            NullnessStore elseStore = res.getElseStore();

            List<Node> secondParts = splitAssignments(secondNode);
            for (Node secondPart : secondParts) {
                Receiver secondInternal =
                        FlowExpressions.internalReprOf(analysis.getTypeFactory(), secondPart);
                if (CFAbstractStore.canInsertReceiver(secondInternal)) {
                    thenStore = thenStore == null ? res.getThenStore() : thenStore;
                    elseStore = elseStore == null ? res.getElseStore() : elseStore;
                    if (notEqualTo) {
                        thenStore.insertValue(secondInternal, NONNULL);
                    } else {
                        elseStore.insertValue(secondInternal, NONNULL);
                    }
                }
            }

            Set<AnnotationMirror> secondAnnos =
                    secondValue != null
                            ? secondValue.getAnnotations()
                            : AnnotationUtils.createAnnotationSet();
            if (AnnotationUtils.containsSameByClass(secondAnnos, PolyNull.class)
                    || AnnotationUtils.containsSameByClass(secondAnnos, PolyAll.class)) {
                thenStore = thenStore == null ? res.getThenStore() : thenStore;
                elseStore = elseStore == null ? res.getElseStore() : elseStore;
                thenStore.setPolyNullNull(true);
            }

            if (thenStore != null) {
                return new ConditionalTransferResult<>(res.getResultValue(), thenStore, elseStore);
            }
        }
        return res;
    }

    @Override
    public TransferResult<NullnessValue, NullnessStore> visitArrayAccess(
            ArrayAccessNode n, TransferInput<NullnessValue, NullnessStore> p) {
        TransferResult<NullnessValue, NullnessStore> result = super.visitArrayAccess(n, p);
        makeNonNull(result, n.getArray());
        return result;
    }

    @Override
    public TransferResult<NullnessValue, NullnessStore> visitInstanceOf(
            InstanceOfNode n, TransferInput<NullnessValue, NullnessStore> p) {
        TransferResult<NullnessValue, NullnessStore> result = super.visitInstanceOf(n, p);
        NullnessStore thenStore = result.getThenStore();
        NullnessStore elseStore = result.getElseStore();
        makeNonNull(thenStore, n.getOperand());
        return new ConditionalTransferResult<>(result.getResultValue(), thenStore, elseStore);
    }

    @Override
    public TransferResult<NullnessValue, NullnessStore> visitMethodAccess(
            MethodAccessNode n, TransferInput<NullnessValue, NullnessStore> p) {
        TransferResult<NullnessValue, NullnessStore> result = super.visitMethodAccess(n, p);
        makeNonNull(result, n.getReceiver());
        return result;
    }

    @Override
    public TransferResult<NullnessValue, NullnessStore> visitFieldAccess(
            FieldAccessNode n, TransferInput<NullnessValue, NullnessStore> p) {
        TransferResult<NullnessValue, NullnessStore> result = super.visitFieldAccess(n, p);
        makeNonNull(result, n.getReceiver());
        return result;
    }

    @Override
    public TransferResult<NullnessValue, NullnessStore> visitThrow(
            ThrowNode n, TransferInput<NullnessValue, NullnessStore> p) {
        TransferResult<NullnessValue, NullnessStore> result = super.visitThrow(n, p);
        makeNonNull(result, n.getExpression());
        return result;
    }

    /*
     * Provided that m is of a type that implements interface java.util.Map:
     * <ul>
     * <li>Given a call m.get(k), if k is @KeyFor("m"), ensures that the result is @NonNull in the thenStore and elseStore of the transfer result.
     * </ul>
     */
    @Override
    public TransferResult<NullnessValue, NullnessStore> visitMethodInvocation(
            MethodInvocationNode n, TransferInput<NullnessValue, NullnessStore> in) {
        TransferResult<NullnessValue, NullnessStore> result = super.visitMethodInvocation(n, in);

        // Make receiver non-null.
        makeNonNull(result, n.getTarget().getReceiver());

        // For all formal parameters with a non-null annotation, make the actual
        // argument non-null.
        MethodInvocationTree tree = n.getTree();
        ExecutableElement method = TreeUtils.elementFromUse(tree);
        AnnotatedExecutableType methodType = analysis.getTypeFactory().getAnnotatedType(method);
        List<AnnotatedTypeMirror> methodParams = methodType.getParameterTypes();
        List<? extends ExpressionTree> methodArgs = tree.getArguments();
        for (int i = 0; i < methodParams.size() && i < methodArgs.size(); ++i) {
            if (methodParams.get(i).hasAnnotation(NONNULL)) {
                makeNonNull(result, n.getArgument(i));
            }
        }

        // Refine result to @NonNull if n is an invocation of Map.get and the argument is a key for
        // the map.
        if (keyForTypeFactory != null && keyForTypeFactory.isInvocationOfMapMethod(n, "get")) {
            Node receiver = n.getTarget().getReceiver();
            String mapName =
                    FlowExpressions.internalReprOf(analysis.getTypeFactory(), receiver).toString();

            if (keyForTypeFactory.isKeyForMap(mapName, methodArgs.get(0))) {
                makeNonNull(result, n);

                NullnessValue oldResultValue = result.getResultValue();
                NullnessValue refinedResultValue =
                        analysis.createSingleAnnotationValue(
                                NONNULL, oldResultValue.getUnderlyingType());
                NullnessValue newResultValue =
                        refinedResultValue.mostSpecific(oldResultValue, null);
                result.setResultValue(newResultValue);
            }
        }

        return result;
    }

    @Override
    public TransferResult<NullnessValue, NullnessStore> visitReturn(
            ReturnNode n, TransferInput<NullnessValue, NullnessStore> in) {
        // HACK: make sure we have a value for return statements, because we
        // need to record whether (at this return statement) isPolyNullNull is
        // set or not.
        NullnessValue value = createDummyValue();
        if (in.containsTwoStores()) {
            NullnessStore thenStore = in.getThenStore();
            NullnessStore elseStore = in.getElseStore();
            return new ConditionalTransferResult<>(
                    finishValue(value, thenStore, elseStore), thenStore, elseStore);
        } else {
            NullnessStore info = in.getRegularStore();
            return new RegularTransferResult<>(finishValue(value, info), info);
        }
    }

    /** Creates a dummy abstract value (whose type is not supposed to be looked at). */
    private NullnessValue createDummyValue() {
        TypeMirror dummy = analysis.getEnv().getTypeUtils().getPrimitiveType(TypeKind.BOOLEAN);
        Set<AnnotationMirror> annos = AnnotationUtils.createAnnotationSet();
        annos.addAll(analysis.getTypeFactory().getQualifierHierarchy().getBottomAnnotations());
        return new NullnessValue(analysis, annos, dummy);
    }
}
