package org.checkerframework.checker.calledmethods;

import org.checkerframework.checker.calledmethods.qual.EnsuresCalledMethodsVarArgs;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.accumulation.AccumulationTransfer;
import org.checkerframework.dataflow.analysis.ConditionalTransferResult;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.block.ExceptionBlock;
import org.checkerframework.dataflow.cfg.node.ArrayCreationNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.framework.flow.CFAbstractStore;
import org.checkerframework.framework.flow.CFAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TreeUtils;
import org.plumelib.util.CollectionsPlume;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

/** A transfer function that accumulates the names of methods called. */
public class CalledMethodsTransfer extends AccumulationTransfer {

    /**
     * {@link #makeExceptionalStores(MethodInvocationNode, TransferInput)} requires a TransferInput,
     * but the actual exceptional stores need to be modified in {@link #accumulate(Node,
     * TransferResult, String...)}, which only has access to a TransferResult. So this field is set
     * to non-null in {@link #visitMethodInvocation(MethodInvocationNode, TransferInput)} via a call
     * to {@link #makeExceptionalStores(MethodInvocationNode, TransferInput)} (which reads the
     * CFStores from the TransferInput) before the call to accumulate(); accumulate() can then use
     * this field to read the CFStores; and then finally this field is then reset to null afterwards
     * to prevent it from being used somewhere it shouldn't be.
     */
    private @Nullable Map<TypeMirror, CFStore> exceptionalStores;

    /**
     * The element for the CalledMethods annotation's value element. Stored in a field in this class
     * to prevent the need to cast to CalledMethods ATF every time it's used.
     */
    private final ExecutableElement calledMethodsValueElement;

    /**
     * Create a new CalledMethodsTransfer.
     *
     * @param analysis the analysis
     */
    public CalledMethodsTransfer(final CFAnalysis analysis) {
        super(analysis);
        calledMethodsValueElement =
                ((CalledMethodsAnnotatedTypeFactory) atypeFactory).calledMethodsValueElement;
    }

    @Override
    public TransferResult<CFValue, CFStore> visitMethodInvocation(
            final MethodInvocationNode node, final TransferInput<CFValue, CFStore> input) {
        exceptionalStores = makeExceptionalStores(node, input);
        TransferResult<CFValue, CFStore> superResult = super.visitMethodInvocation(node, input);
        handleEnsuresCalledMethodsVarArgs(node, superResult);
        Node receiver = node.getTarget().getReceiver();
        if (receiver != null) {
            String methodName = node.getTarget().getMethod().getSimpleName().toString();
            methodName =
                    ((CalledMethodsAnnotatedTypeFactory) atypeFactory)
                            .adjustMethodNameUsingValueChecker(methodName, node.getTree());
            accumulate(receiver, superResult, methodName);
        }
        TransferResult<CFValue, CFStore> finalResult =
                new ConditionalTransferResult<>(
                        superResult.getResultValue(),
                        superResult.getThenStore(),
                        superResult.getElseStore(),
                        exceptionalStores);
        exceptionalStores = null;
        return finalResult;
    }

    @Override
    public void accumulate(Node node, TransferResult<CFValue, CFStore> result, String... values) {
        super.accumulate(node, result, values);
        if (exceptionalStores == null) {
            return;
        }

        List<String> valuesAsList = Arrays.asList(values);
        JavaExpression target = JavaExpression.fromNode(node);
        if (CFAbstractStore.canInsertJavaExpression(target)) {
            CFValue flowValue = result.getRegularStore().getValue(target);
            if (flowValue != null) {
                // Dataflow has already recorded information about the target.  Integrate it into
                // the list of values in the new annotation.
                Set<AnnotationMirror> flowAnnos = flowValue.getAnnotations();
                assert flowAnnos.size() <= 1;
                for (AnnotationMirror anno : flowAnnos) {
                    if (atypeFactory.isAccumulatorAnnotation(anno)) {
                        List<String> oldFlowValues =
                                AnnotationUtils.getElementValueArray(
                                        anno, calledMethodsValueElement, String.class);
                        // valuesAsList cannot have its length changed -- it is backed by an
                        // array.  getElementValueArray returns a new, modifiable list.
                        oldFlowValues.addAll(valuesAsList);
                        valuesAsList = oldFlowValues;
                    }
                }
            }
            AnnotationMirror newAnno = atypeFactory.createAccumulatorAnnotation(valuesAsList);
            exceptionalStores.forEach((tm, s) -> s.insertValue(target, newAnno));
        }
    }

    /**
     * Create a set of stores for the exceptional paths out of the block containing {@code node}.
     * This allows propagation, along those paths, of the fact that the method being invoked in
     * {@code node} was definitely called.
     *
     * @param node a method invocation
     * @param input the transfer input associated with the method invocation
     * @return a map from types to stores. The keys are the same keys used by {@link
     *     ExceptionBlock#getExceptionalSuccessors()}. The values are copies of the regular store
     *     from {@code input}.
     */
    private Map<TypeMirror, CFStore> makeExceptionalStores(
            MethodInvocationNode node, final TransferInput<CFValue, CFStore> input) {
        if (!(node.getBlock() instanceof ExceptionBlock)) {
            // This can happen in some weird (buggy?) cases:
            // see https://github.com/typetools/checker-framework/issues/3585
            return Collections.emptyMap();
        }
        ExceptionBlock block = (ExceptionBlock) node.getBlock();
        Map<TypeMirror, CFStore> result = new LinkedHashMap<>();
        block.getExceptionalSuccessors()
                .forEach((tm, b) -> result.put(tm, input.getRegularStore().copy()));
        return result;
    }

    /**
     * Update the types of varargs parameters passed to a method with an {@link
     * EnsuresCalledMethodsVarArgs} annotation. This method is a no-op if no such annotation is
     * present.
     *
     * @param node the method invocation node
     * @param result the current result
     */
    private void handleEnsuresCalledMethodsVarArgs(
            MethodInvocationNode node, TransferResult<CFValue, CFStore> result) {
        ExecutableElement elt = TreeUtils.elementFromUse(node.getTree());
        AnnotationMirror annot =
                atypeFactory.getDeclAnnotation(elt, EnsuresCalledMethodsVarArgs.class);
        if (annot == null) {
            return;
        }
        List<String> ensuredMethodNames =
                AnnotationUtils.getElementValueArray(
                        annot,
                        ((CalledMethodsAnnotatedTypeFactory) atypeFactory)
                                .ensuresCalledMethodsVarArgsValueElement,
                        String.class);
        List<? extends VariableElement> parameters = elt.getParameters();
        int varArgsPos = parameters.size() - 1;
        Node varArgActual = node.getArguments().get(varArgsPos);
        // In the CFG, explicit passing of multiple arguments in the varargs position is represented
        // via an ArrayCreationNode.  This is the only case we handle for now.
        if (varArgActual instanceof ArrayCreationNode) {
            ArrayCreationNode arrayCreationNode = (ArrayCreationNode) varArgActual;
            // add in the called method to all the vararg arguments
            CFStore thenStore = result.getThenStore();
            CFStore elseStore = result.getElseStore();
            for (Node arg : arrayCreationNode.getInitializers()) {
                AnnotatedTypeMirror currentType = atypeFactory.getAnnotatedType(arg.getTree());
                AnnotationMirror newType =
                        getUpdatedCalledMethodsType(currentType, ensuredMethodNames);
                if (newType == null) {
                    continue;
                }

                JavaExpression receiverReceiver = JavaExpression.fromNode(arg);
                thenStore.insertValue(receiverReceiver, newType);
                elseStore.insertValue(receiverReceiver, newType);
            }
        }
    }

    /**
     * Extract the current called-methods type from {@code currentType}, and then add each element
     * of {@code methodNames} to it, and return the result. This method is similar to GLB, but
     * should be used when the new methods come from a source other than an {@code CalledMethods}
     * annotation.
     *
     * @param currentType the current type in the called-methods hierarchy
     * @param methodNames the names of the new methods to add to the type
     * @return the new annotation to be added to the type, or null if the current type cannot be
     *     converted to an accumulator annotation
     */
    private @Nullable AnnotationMirror getUpdatedCalledMethodsType(
            AnnotatedTypeMirror currentType, List<String> methodNames) {
        AnnotationMirror type;
        if (currentType == null || !currentType.isAnnotatedInHierarchy(atypeFactory.top)) {
            type = atypeFactory.top;
        } else {
            type = currentType.getAnnotationInHierarchy(atypeFactory.top);
        }

        // Don't attempt to strengthen @CalledMethodsPredicate annotations, because that would
        // require reasoning about the predicate itself. Instead, start over from top.
        if (AnnotationUtils.areSameByName(
                type, "org.checkerframework.checker.calledmethods.qual.CalledMethodsPredicate")) {
            type = atypeFactory.top;
        }

        if (AnnotationUtils.areSame(type, atypeFactory.bottom)) {
            return null;
        }

        List<String> currentMethods =
                AnnotationUtils.getElementValueArray(type, calledMethodsValueElement, String.class);
        List<String> newList = CollectionsPlume.concatenate(currentMethods, methodNames);

        return atypeFactory.createAccumulatorAnnotation(newList);
    }
}
