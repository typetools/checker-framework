package org.checkerframework.checker.determinism;

import java.util.Comparator;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
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
        ExecutableElement invokedMethod = n.getTarget().getMethod();

        // Type refinement for List.sort
        if (isListSort(factory, underlyingTypeOfReceiver, invokedMethod)) {
            if (factory.getAnnotatedType(receiver.getTree()).hasAnnotation(OrderNonDet.class)) {
                typeRefine(n.getTarget().getReceiver(), result, factory.DET, factory);
            }
        }

        // Type refinement for Arrays.sort() and Arrays.parallelSort()
        // For Arrays.sort(int[] a), refines 'a' to @Det[..] if 'a' is of the type @OrderNonDet[..]
        // For Arrays.sort(int[] a, int fromIndex, int toIndex), Arrays.sort(T[], Comparator<? super
        // T> c),
        // and all other variants of Arrays.sort() and Arrays.parallelSort(),
        // refines the first argument if this first argument is annotated as @OrderNonDet[..]
        // and all other arguments are annotated as @Det.
        if (isArraysSort(factory, underlyingTypeOfReceiver, invokedMethod)) {
            AnnotatedTypeMirror firstArg =
                    factory.getAnnotatedType(n.getTree().getArguments().get(0));
            if (firstArg.hasAnnotation(factory.ORDERNONDET)) {
                // The following code sets the flag typeRefine to true if
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
                    typeRefine(n.getArgument(0), result, factory.DET, factory);
                }
            }
        }

        // Type refinement for Collections.sort
        if (isCollectionsSort(factory, underlyingTypeOfReceiver, invokedMethod)) {
            AnnotatedTypeMirror firstArg =
                    factory.getAnnotatedType(n.getTree().getArguments().get(0));
            if (firstArg.hasAnnotation(factory.ORDERNONDET)) {
                typeRefine(n.getArgument(0), result, factory.DET, factory);
            }
        }

        // Type refinement for Collections.shuffle
        if (isCollectionsShuffle(factory, underlyingTypeOfReceiver, invokedMethod)) {
            AnnotatedTypeMirror firstArg =
                    factory.getAnnotatedType(n.getTree().getArguments().get(0));
            if (firstArg.hasAnnotation(factory.DET)) {
                typeRefine(n.getArgument(0), result, factory.ORDERNONDET, factory);
            }
        }

        return result;
    }

    /**
     * Checks if the receiver is a List or a subtype of List and the method invoked is sort().
     *
     * @param factory the determinism factory
     * @param underlyingTypeOfReceiver the underlying type of the receiver (TypeMirror)
     * @param invokedMethod the invoked method name
     * @return true if the receiver is a List and the method invoked is sort(), false otherwise
     */
    private boolean isListSort(
            DeterminismAnnotatedTypeFactory factory,
            TypeMirror underlyingTypeOfReceiver,
            ExecutableElement invokedMethod) {
        ProcessingEnvironment env = factory.getProcessingEnv();
        Types types = env.getTypeUtils();
        return (factory.isList(underlyingTypeOfReceiver)
                && invokedMethod.getSimpleName().contentEquals("sort")
                && invokedMethod.getReturnType().getKind() == TypeKind.VOID
                && invokedMethod.getParameters().size() == 1
                && types.isSameType(
                        types.erasure(invokedMethod.getParameters().get(0).asType()),
                        types.erasure(
                                TypesUtils.typeFromClass(
                                        Comparator.class, types, env.getElementUtils()))));
    }

    /**
     * Checks if the invoked method is Arrays.sort().
     *
     * @param factory the determinism factory
     * @param underlyingTypeOfReceiver the underlying type of the receiver (TypeMirror)
     * @param invokedMethod the invoked method name
     * @return true if the invoked method is Arrays.sort(), false otherwise
     */
    private boolean isArraysSort(
            DeterminismAnnotatedTypeFactory factory,
            TypeMirror underlyingTypeOfReceiver,
            ExecutableElement invokedMethod) {
        return (factory.isArrays(underlyingTypeOfReceiver)
                && (invokedMethod.getSimpleName().contentEquals("sort")
                        || invokedMethod.getSimpleName().contentEquals("parallelSort")));
    }

    /**
     * Checks if the invoked method is Collections.sort().
     *
     * @param factory the determinism factory
     * @param underlyingTypeOfReceiver the underlying type of the receiver (TypeMirror)
     * @param invokedMethod the invoked method name
     * @return true if the invoked method is Collections.sort(), false otherwise
     */
    private boolean isCollectionsSort(
            DeterminismAnnotatedTypeFactory factory,
            TypeMirror underlyingTypeOfReceiver,
            ExecutableElement invokedMethod) {
        return (factory.isCollections(underlyingTypeOfReceiver)
                && invokedMethod.getSimpleName().contentEquals("sort"));
    }

    /**
     * Checks if the invoked method is Collections.shuffle().
     *
     * @param factory the determinism factory
     * @param underlyingTypeOfReceiver the underlying type of the receiver (TypeMirror)
     * @param invokedMethod the invoked method name
     * @return true if the invoked method is Collections.shuffle(), false otherwise
     */
    private boolean isCollectionsShuffle(
            DeterminismAnnotatedTypeFactory factory,
            TypeMirror underlyingTypeOfReceiver,
            ExecutableElement invokedMethod) {
        return (factory.isCollections(underlyingTypeOfReceiver)
                && invokedMethod.getSimpleName().contentEquals("shuffle"));
    }

    /**
     * Refines the type of {@code node} to {@code replaceType}.
     *
     * @param node the node to be refined
     * @param result the determinism transfer result store
     * @param replaceType the type to be refined with
     * @param factory the determinism factory
     */
    private void typeRefine(
            // TODO-rashmi: type refinement for first argument of Collections.shuffle() from
            // @Det to @OrderNonDet doesn't seem to work.
            // result.getThenStore().insertValue(receiver, replaceType); replaces the
            // annotation on the 'receiver' if 'replaceType' is stronger than
            // the current annotation of 'receiver'.
            // The annotation on first argument of shuffle (@Det) is stronger than 'replaceType'
            // (@OrderNonDet).
            Node node,
            TransferResult<CFValue, CFStore> result,
            AnnotationMirror replaceType,
            DeterminismAnnotatedTypeFactory factory) {
        FlowExpressions.Receiver receiver = FlowExpressions.internalReprOf(factory, node);
        result.getThenStore().insertValue(receiver, replaceType);
        result.getElseStore().insertValue(receiver, replaceType);
    }
}
