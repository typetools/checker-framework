package org.checkerframework.checker.determinism;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Name;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFTransfer;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TypesUtils;

/**
 * Transfer function for the determinism type-system.
 *
 * <p>Performs type refinement from {@code @OrderNonDet} to {@code @Det} for:
 *
 * <ul>
 *   <li>The receiver of List.sort.
 *   <li>The first argument of Arrays.sort.
 *   <li>The first argument of Arrays.parallelSort.
 *   <li>The first argument of Collections.sort.
 *   <li>The first argument of Collections.shuffle.
 * </ul>
 */
public class DeterminismTransfer extends CFTransfer {
    public DeterminismTransfer(CFAbstractAnalysis<CFValue, CFStore, CFTransfer> analysis) {
        super(analysis);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitMethodInvocation(
            MethodInvocationNode n, TransferInput<CFValue, CFStore> in) {
        TransferResult<CFValue, CFStore> result = super.visitMethodInvocation(n, in);
        DeterminismAnnotatedTypeFactory factory =
                (DeterminismAnnotatedTypeFactory) analysis.getTypeFactory();

        // Note: For static method calls, the receiver is the Class node
        // that declares the method.
        Node receiver = n.getTarget().getReceiver();

        // TypesUtils.getTypeElement(receiver.getType()) is null for generic type arguments.
        if (TypesUtils.getTypeElement(receiver.getType()) == null) {
            return result;
        }

        TypeMirror underlyingTypeOfReceiver =
                TypesUtils.getTypeElement(receiver.getType()).asType();
        Name methName = n.getTarget().getMethod().getSimpleName();

        AnnotationMirror refineWithType = factory.DET;
        boolean refineReceiver = false;
        boolean refineArgument = false;

        // Type refinement for List sort
        if (isListSort(factory, receiver, underlyingTypeOfReceiver, methName)) {
            AnnotationMirror receiverAnno =
                    receiver.getType().getAnnotationMirrors().iterator().next();
            if (receiverAnno != null
                    && AnnotationUtils.areSame(receiverAnno, factory.ORDERNONDET)) {
                refineReceiver = true;
            }
        }

        // Type refinement for Arrays sort
        if (isArraysSort(factory, underlyingTypeOfReceiver, methName)) {
            AnnotatedTypeMirror firstArg =
                    factory.getAnnotatedType(n.getTree().getArguments().get(0));
            AnnotationMirror firstArgAnno = firstArg.getAnnotations().iterator().next();
            if (firstArgAnno != null
                    && AnnotationUtils.areSame(firstArgAnno, factory.ORDERNONDET)) {

                // Consider the call to Arrays.sort(T[], Comparator<? super T> c)
                // The first argument of this method invocation must be type-refined
                // only if it is annotated as @OrderNonDet and the second argument
                // is annotated @Det (Not if it is @NonDet).
                // The following code sets the flag typeRefine to true iff
                // all arguments except the first are annotated as @Det.
                boolean typeRefine = true;
                for (int i = 1; i < n.getArguments().size(); i++) {
                    AnnotatedTypeMirror otherArgType =
                            factory.getAnnotatedType(n.getTree().getArguments().get(i));
                    if (!otherArgType.hasAnnotation(factory.DET)) {
                        typeRefine = false;
                        break;
                    }
                }
                if (typeRefine) {
                    refineArgument = true;
                }
            }
        }

        // Type refinement for Collections sort
        if (isCollectionsSort(factory, underlyingTypeOfReceiver, methName)) {
            AnnotatedTypeMirror firstArg =
                    factory.getAnnotatedType(n.getTree().getArguments().get(0));
            AnnotationMirror firstArgAnno = firstArg.getAnnotations().iterator().next();
            if (firstArgAnno != null
                    && AnnotationUtils.areSame(firstArgAnno, factory.ORDERNONDET)) {
                refineArgument = true;
            }
        }

        // Type refinement for Collections shuffle
        if (isCollectionsShuffle(factory, underlyingTypeOfReceiver, methName)) {
            refineArgument = true;
            refineWithType = factory.NONDET;
        }

        Node receiverToBeRefined = null;
        if (refineReceiver) {
            receiverToBeRefined = n.getTarget().getReceiver();
        }
        if (refineArgument) {
            receiverToBeRefined = n.getArgument(0);
        }

        typeRefine(receiverToBeRefined, result, refineWithType, factory);
        return result;
    }

    /**
     * Checks if the receiver is a List and the method invoked is sort().
     *
     * @param factory the determinism factory
     * @param receiver the receiver Node
     * @param underlyingTypeOfReceiver the underlying type of the receiver (TypeMirror)
     * @param methName the invoked method name
     * @return true if the receiver is a List and the method invoked is sort(), false otherwise
     */
    private boolean isListSort(
            DeterminismAnnotatedTypeFactory factory,
            Node receiver,
            TypeMirror underlyingTypeOfReceiver,
            Name methName) {
        return (factory.isList(underlyingTypeOfReceiver)
                && methName.contentEquals("sort")
                && receiver.getType().getAnnotationMirrors().size() > 0);
    }

    /**
     * Checks if the invoked method is Arrays.sort().
     *
     * @param factory the determinism factory
     * @param underlyingTypeOfReceiver the underlying type of the receiver (TypeMirror)
     * @param methName the invoked method name
     * @return true if the invoked method is Arrays.sort(), false otherwise
     */
    private boolean isArraysSort(
            DeterminismAnnotatedTypeFactory factory,
            TypeMirror underlyingTypeOfReceiver,
            Name methName) {

        return (factory.isArrays(underlyingTypeOfReceiver)
                && (methName.contentEquals("sort") || methName.contentEquals("parallelSort")));
    }

    /**
     * Checks if the invoked method is Collections.sort().
     *
     * @param factory the determinism factory
     * @param underlyingTypeOfReceiver the underlying type of the receiver (TypeMirror)
     * @param methName the invoked method name
     * @return true if the invoked method is Collections.sort(), false otherwise
     */
    private boolean isCollectionsSort(
            DeterminismAnnotatedTypeFactory factory,
            TypeMirror underlyingTypeOfReceiver,
            Name methName) {
        return (factory.isCollections(underlyingTypeOfReceiver) && methName.contentEquals("sort"));
    }

    /**
     * Checks if the invoked method is Collections.shuffle().
     *
     * @param factory the determinism factory
     * @param underlyingTypeOfReceiver the underlying type of the receiver (TypeMirror)
     * @param methName the invoked method name
     * @return true if the invoked method is Collections.shuffle(), false otherwise
     */
    private boolean isCollectionsShuffle(
            DeterminismAnnotatedTypeFactory factory,
            TypeMirror underlyingTypeOfReceiver,
            Name methName) {
        return (factory.isCollections(underlyingTypeOfReceiver)
                && methName.contentEquals("shuffle"));
    }

    /**
     * Helper method for type refinement.
     *
     * @param node the node to be refined
     * @param result the determinism transfer result store
     * @param replaceType the type to be refined with
     * @param factory the determinism factory
     */
    private void typeRefine(
            Node node,
            TransferResult<CFValue, CFStore> result,
            AnnotationMirror replaceType,
            DeterminismAnnotatedTypeFactory factory) {
        if (node == null) return;
        FlowExpressions.Receiver receiver = FlowExpressions.internalReprOf(factory, node);
        result.getThenStore().insertValue(receiver, replaceType);
        result.getElseStore().insertValue(receiver, replaceType);
    }
}
