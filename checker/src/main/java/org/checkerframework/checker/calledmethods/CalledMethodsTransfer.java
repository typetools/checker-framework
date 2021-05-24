package org.checkerframework.checker.calledmethods;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.accumulation.AccumulationTransfer;
import org.checkerframework.dataflow.analysis.ConditionalTransferResult;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.block.ExceptionBlock;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.framework.flow.CFAbstractStore;
import org.checkerframework.framework.flow.CFAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.javacutil.AnnotationUtils;

/** A transfer function that accumulates the names of methods called. */
public class CalledMethodsTransfer extends AccumulationTransfer {

  /**
   * {@link #makeExceptionalStores(MethodInvocationNode, TransferInput)} requires a TransferInput,
   * but the actual exceptional stores need to be modified in {@link #accumulate(Node,
   * TransferResult, String...)}, which only has access to a TransferResult. So this field is set to
   * non-null in {@link #visitMethodInvocation(MethodInvocationNode, TransferInput)} via a call to
   * {@link #makeExceptionalStores(MethodInvocationNode, TransferInput)} (which reads the CFStores
   * from the TransferInput) before the call to accumulate(); accumulate() can then use this field
   * to read the CFStores; and then finally this field is then reset to null afterwards to prevent
   * it from being used somewhere it shouldn't be.
   */
  private @Nullable Map<TypeMirror, CFStore> exceptionalStores;

  /**
   * Create a new CalledMethodsTransfer.
   *
   * @param analysis the analysis
   */
  public CalledMethodsTransfer(final CFAnalysis analysis) {
    super(analysis);
  }

  @Override
  public TransferResult<CFValue, CFStore> visitMethodInvocation(
      final MethodInvocationNode node, final TransferInput<CFValue, CFStore> input) {
    exceptionalStores = makeExceptionalStores(node, input);
    TransferResult<CFValue, CFStore> superResult = super.visitMethodInvocation(node, input);
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
        // Dataflow has already recorded information about the target.  Integrate it into the list
        // of values in the new annotation.
        Set<AnnotationMirror> flowAnnos = flowValue.getAnnotations();
        assert flowAnnos.size() <= 1;
        for (AnnotationMirror anno : flowAnnos) {
          if (atypeFactory.isAccumulatorAnnotation(anno)) {
            List<String> oldFlowValues =
                AnnotationUtils.getElementValueArray(
                    anno,
                    ((CalledMethodsAnnotatedTypeFactory) atypeFactory).calledMethodsValueElement,
                    String.class);
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
   * Create a set of stores for the exceptional paths out of the block containing {@code node}. This
   * allows propagation, along those paths, of the fact that the method being invoked in {@code
   * node} was definitely called.
   *
   * @param node a method invocation
   * @param input the transfer input associated with the method invocation
   * @return a map from types to stores. The keys are the same keys used by {@link
   *     ExceptionBlock#getExceptionalSuccessors()}. The values are copies of the regular store from
   *     {@code input}.
   */
  private Map<TypeMirror, CFStore> makeExceptionalStores(
      MethodInvocationNode node, final TransferInput<CFValue, CFStore> input) {
    if (!(node.getBlock() instanceof ExceptionBlock)) {
      // this can happen in some weird (buggy?) cases
      return Collections.emptyMap();
    }
    ExceptionBlock block = (ExceptionBlock) node.getBlock();
    Map<TypeMirror, CFStore> result = new LinkedHashMap<>();
    block
        .getExceptionalSuccessors()
        .forEach((tm, b) -> result.put(tm, input.getRegularStore().copy()));
    return result;
  }
}
