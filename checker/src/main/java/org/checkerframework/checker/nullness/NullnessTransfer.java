package org.checkerframework.checker.nullness;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
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
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFAbstractStore;
import org.checkerframework.framework.qual.PolyAll;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;

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

    /** The @{@link NonNull} annotation. */
    protected final AnnotationMirror NONNULL;
    /** The @{@link Nullable} annotation. */
    protected final AnnotationMirror NULLABLE;

    /**
     * Java's Map interface.
     *
     * <p>The qualifiers in this type don't matter -- it is not used as a fully-annotated
     * AnnotatedDeclaredType, but just passed to asSuper().
     */
    protected final AnnotatedDeclaredType MAP_TYPE;

    /** The type factory for the nullness analysis that was passed to the constructor. */
    protected final GenericAnnotatedTypeFactory<
                    NullnessValue,
                    NullnessStore,
                    NullnessTransfer,
                    ? extends CFAbstractAnalysis<NullnessValue, NullnessStore, NullnessTransfer>>
            nullnessTypeFactory;

    /** The type factory for the map key analysis. */
    protected final KeyForAnnotatedTypeFactory keyForTypeFactory;

    /** Create a new NullnessTransfer for the given analysis. */
    public NullnessTransfer(NullnessAnalysis analysis) {
        super(analysis);
        this.nullnessTypeFactory = analysis.getTypeFactory();
        Elements elements = nullnessTypeFactory.getElementUtils();
        this.keyForTypeFactory =
                ((BaseTypeChecker) nullnessTypeFactory.getContext().getChecker())
                        .getTypeFactoryOfSubchecker(KeyForSubchecker.class);
        NONNULL = AnnotationBuilder.fromClass(elements, NonNull.class);
        NULLABLE = AnnotationBuilder.fromClass(elements, Nullable.class);

        MAP_TYPE =
                (AnnotatedDeclaredType)
                        AnnotatedTypeMirror.createType(
                                TypesUtils.typeFromClass(Map.class, analysis.getTypes(), elements),
                                nullnessTypeFactory,
                                false);
    }

    /**
     * Sets a given {@link Node} to non-null in the given {@code store}. Calls to this method
     * implement case 2.
     */
    protected void makeNonNull(NullnessStore store, Node node) {
        Receiver internalRepr = FlowExpressions.internalReprOf(nullnessTypeFactory, node);
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

    /** Refine the given result to @NonNull. */
    protected void refineToNonNull(TransferResult<NullnessValue, NullnessStore> result) {
        NullnessValue oldResultValue = result.getResultValue();
        NullnessValue refinedResultValue =
                analysis.createSingleAnnotationValue(NONNULL, oldResultValue.getUnderlyingType());
        NullnessValue newResultValue = refinedResultValue.mostSpecific(oldResultValue, null);
        result.setResultValue(newResultValue);
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
                        FlowExpressions.internalReprOf(nullnessTypeFactory, secondPart);
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
     * <li>Given a call m.get(k), if k is @KeyFor("m") and m's value type is @NonNull,
     *     then the result is @NonNull in the thenStore and elseStore of the transfer result.
     * </ul>
     */
    @Override
    public TransferResult<NullnessValue, NullnessStore> visitMethodInvocation(
            MethodInvocationNode n, TransferInput<NullnessValue, NullnessStore> in) {
        TransferResult<NullnessValue, NullnessStore> result = super.visitMethodInvocation(n, in);

        // Make receiver non-null.
        Node receiver = n.getTarget().getReceiver();
        makeNonNull(result, receiver);

        // For all formal parameters with a non-null annotation, make the actual argument non-null.
        // The point of this is to prevent cascaded errors -- the Nullness Checker will issue a
        // warning for the method invocation, but not for subsequent uses of the argument.  See test
        // case FlowNullness.java.
        MethodInvocationTree tree = n.getTree();
        ExecutableElement method = TreeUtils.elementFromUse(tree);
        AnnotatedExecutableType methodType = nullnessTypeFactory.getAnnotatedType(method);
        List<AnnotatedTypeMirror> methodParams = methodType.getParameterTypes();
        List<? extends ExpressionTree> methodArgs = tree.getArguments();
        for (int i = 0; i < methodParams.size() && i < methodArgs.size(); ++i) {
            if (methodParams.get(i).hasAnnotation(NONNULL)) {
                makeNonNull(result, n.getArgument(i));
            }
        }

        // Refine result to @NonNull if n is an invocation of Map.get, the argument is a key for
        // the map, and the map's value type is not @Nullable.
        if (keyForTypeFactory != null && keyForTypeFactory.isMapGet(n)) {
            String mapName =
                    FlowExpressions.internalReprOf(nullnessTypeFactory, receiver).toString();
            AnnotatedTypeMirror receiverType = nullnessTypeFactory.getReceiverType(n.getTree());

            if (keyForTypeFactory.isKeyForMap(mapName, methodArgs.get(0))
                    && !hasNullableValueType((AnnotatedDeclaredType) receiverType)) {
                makeNonNull(result, n);
                refineToNonNull(result);
            }
        }

        return result;
    }

    /**
     * Returns true if mapType's value type (the V type argument to Map) is @Nullable.
     *
     * @param mapOrSubtype the Map type, or a subtype
     * @return true if mapType's value type is @Nullable
     */
    private boolean hasNullableValueType(AnnotatedDeclaredType mapOrSubtype) {
        AnnotatedDeclaredType mapType =
                AnnotatedTypes.asSuper(nullnessTypeFactory, mapOrSubtype, MAP_TYPE);
        int numTypeArguments = mapType.getTypeArguments().size();
        if (numTypeArguments != 2) {
            throw new BugInCF("Wrong number %d of type arguments: %s", numTypeArguments, mapType);
        }
        AnnotatedTypeMirror valueType = mapType.getTypeArguments().get(1);
        return valueType.hasAnnotation(NULLABLE);
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
        annos.addAll(nullnessTypeFactory.getQualifierHierarchy().getBottomAnnotations());
        return new NullnessValue(analysis, annos, dummy);
    }
}
