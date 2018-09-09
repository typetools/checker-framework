package org.checkerframework.checker.determinism;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Name;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.determinism.qual.OrderNonDet;
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
import org.checkerframework.javacutil.TypesUtils;

// TODO: It would be better to put TODO comments near the relevant code.  It took me a while to
// find this in the helper method typeRefine().
// TODO-rashmi: type refinement for first argument of Collections.shuffle() from
// @Det to @OrderNonDet doesn't seem to work.
// result.getThenStore().insertValue(receiver, replaceType); replaces the
// annotation on the receiver with the strongest of current annotation on receiver
// and 'replaceType' in the lattice.
// The annotation first argument of shuffle (@Det) is stronger than replaceType (@OrderNonDet).

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
 * </ul>
 *
 * <p>Performs type refinement from {@code @Det} to {@code @OrderNonDet} for
 *
 * <ul>
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

        // Note: For static method calls, the receiver is the Class that declares the method.
        Node receiver = n.getTarget().getReceiver();

        // TypesUtils.getTypeElement(receiver.getType()) is null for generic type arguments.
        if (TypesUtils.getTypeElement(receiver.getType()) == null) {
            // Why is this return statement correct?
            // Is it because none of the 5 cases has a generic type argument?
            return result;
        }

        TypeMirror underlyingTypeOfReceiver =
                TypesUtils.getTypeElement(receiver.getType()).asType();
        Name methName = n.getTarget().getMethod().getSimpleName();

        // Type refinement for List.sort
        if (isListSort(factory, receiver, underlyingTypeOfReceiver, methName)) {
            // TODO: Why does this use getAnnotation(Class) and then a test against null?
            // I think it would be clearer to use hasAnnotation(ORDERNONDET).  Is that not possible?
            AnnotationMirror receiverAnno =
                    factory.getAnnotatedType(receiver.getTree()).getAnnotation(OrderNonDet.class);
            if (receiverAnno != null) {
                typeRefine(n.getTarget().getReceiver(), result, factory.DET, factory);
            }
        }

        // Type refinement for Arrays.sort
        if (isArraysSort(factory, underlyingTypeOfReceiver, methName)) {
            AnnotatedTypeMirror firstArg =
                    factory.getAnnotatedType(n.getTree().getArguments().get(0));
            if (firstArg.hasAnnotation(factory.ORDERNONDET)) {

                // TODO: It is confusing, that the comment mentions only Arrays.sort(T[],
                // Comparator) but the code also handles Arrays.sort(T[]).
                // Consider the call to Arrays.sort(T[], Comparator<? super T> c)
                // The first argument of this method invocation must be type-refined
                // iff it is annotated as @OrderNonDet and the second argument
                // is annotated @Det (Not if it is @NonDet).
                // The following code sets the flag typeRefine to true iff
                // all arguments except the first are annotated as @Det.
                // TODO: There are only two possibilities for "all arguments except the first":
                // there are 0 or 1 of them.  It would be clearer to handle those two cases with an
                // if statement rather than a for loop that you expect to iterate exactly 0 or 1
                // times.
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
                    typeRefine(n.getArgument(0), result, factory.DET, factory);
                }
            }
        }

        // Type refinement for Collections.sort
        if (isCollectionsSort(factory, underlyingTypeOfReceiver, methName)) {
            AnnotatedTypeMirror firstArg =
                    factory.getAnnotatedType(n.getTree().getArguments().get(0));
            if (firstArg.hasAnnotation(factory.ORDERNONDET)) {
                typeRefine(n.getArgument(0), result, factory.DET, factory);
            }
        }

        // Type refinement for Collections.shuffle
        if (isCollectionsShuffle(factory, underlyingTypeOfReceiver, methName)) {
            AnnotatedTypeMirror firstArg =
                    factory.getAnnotatedType(n.getTree().getArguments().get(0));
            if (firstArg.hasAnnotation(factory.DET)) {
                typeRefine(n.getArgument(0), result, factory.ORDERNONDET, factory);
            }
        }

        return result;
    }

    // Should "is a List" also include subtypes?  The wording implies not.
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
                // TODO: This test is wrong.  The size should be exactly 1 or 2, and the arguments
                // must be of specific types.  Otherwise the checker will behave oddly if a
                // programmer extends list and defines his or her own sort method.
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

    // TODO: "Helper method for type refinement." doesn't say anything about what this method does.
    // A todo comment indicates that there may be a problem with it, but without documentation I
    // cannot review it.
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
        FlowExpressions.Receiver receiver = FlowExpressions.internalReprOf(factory, node);
        result.getThenStore().insertValue(receiver, replaceType);
        result.getElseStore().insertValue(receiver, replaceType);
    }
}
