package org.checkerframework.checker.calledmethodsonelements;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.mustcallonelements.MustCallOnElementsAnnotatedTypeFactory;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.resourceleak.ResourceLeakChecker;
import org.checkerframework.common.accumulation.AccumulationStore;
import org.checkerframework.common.accumulation.AccumulationTransfer;
import org.checkerframework.common.accumulation.AccumulationValue;
import org.checkerframework.dataflow.analysis.ConditionalTransferResult;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.LessThanNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.framework.flow.CFAbstractStore;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.AnnotationMirrorSet;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ElementUtils;
import org.plumelib.util.CollectionsPlume;

/** A transfer function that accumulates the names of methods called. */
public class CalledMethodsOnElementsTransfer extends AccumulationTransfer {

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
  private @Nullable Map<TypeMirror, AccumulationStore> exceptionalStores;

  /**
   * The element for the CalledMethodsOnElements annotation's value element. Stored in a field in
   * this class to prevent the need to cast to CalledMethodsOnElements ATF every time it's used.
   */
  private final ExecutableElement calledMethodsOnElementsValueElement;

  /** The type mirror for {@link Exception}. */
  protected final TypeMirror javaLangExceptionType;

  /**
   * True if -AenableWpiForRlc was passed on the command line. See {@link
   * ResourceLeakChecker#ENABLE_WPI_FOR_RLC}.
   */
  private final boolean enableWpiForRlc;

  /**
   * Create a new CalledMethodsOnElementsTransfer.
   *
   * @param analysis the analysis
   */
  public CalledMethodsOnElementsTransfer(CalledMethodsOnElementsAnalysis analysis) {
    super(analysis);
    calledMethodsOnElementsValueElement =
        ((CalledMethodsOnElementsAnnotatedTypeFactory) atypeFactory)
            .calledMethodsOnElementsValueElement;
    enableWpiForRlc = atypeFactory.getChecker().hasOption(ResourceLeakChecker.ENABLE_WPI_FOR_RLC);

    ProcessingEnvironment env = atypeFactory.getProcessingEnv();
    javaLangExceptionType =
        env.getTypeUtils().getDeclaredType(ElementUtils.getTypeElement(env, Exception.class));
  }

  //   /**
  //    * @param tree a tree
  //    * @return false if Resource Leak Checker is running as one of the upstream checkers and the
  //    *     -AenableWpiForRlc flag (see {@link ResourceLeakChecker#ENABLE_WPI_FOR_RLC}) is not
  // passed
  //    *     as a command line argument, otherwise returns the result of the super call
  //    */
  //   @Override
  //   protected boolean shouldPerformWholeProgramInference(Tree tree) {
  //     if (!isWpiEnabledForRLC()
  //         &&
  // atypeFactory.getCheckerNames().contains(ResourceLeakChecker.class.getCanonicalName())) {
  //       return false;
  //     }
  //     return super.shouldPerformWholeProgramInference(tree);
  //   }

  //   /**
  //    * See {@link ResourceLeakChecker#ENABLE_WPI_FOR_RLC}.
  //    *
  //    * @param expressionTree a tree
  //    * @param lhsTree its element
  //    * @return false if Resource Leak Checker is running as one of the upstream checkers and the
  //    *     -AenableWpiForRlc flag is not passed as a command line argument, otherwise returns the
  //    *     result of the super call
  //    */
  //   @Override
  //   protected boolean shouldPerformWholeProgramInference(Tree expressionTree, Tree lhsTree) {
  //     if (!isWpiEnabledForRLC()
  //         &&
  // atypeFactory.getCheckerNames().contains(ResourceLeakChecker.class.getCanonicalName())) {
  //       return false;
  //     }
  //     return super.shouldPerformWholeProgramInference(expressionTree, lhsTree);
  //   }

  /**
   * {@inheritDoc}
   *
   * <p>Furthermore, this method refines the type to {@code NonNull} for the appropriate branch if
   * an expression is compared to the {@code null} literal (listed as case 1 in the class
   * description).
   */
  @Override
  public TransferResult<AccumulationValue, AccumulationStore> visitLessThan(
      LessThanNode node, TransferInput<AccumulationValue, AccumulationStore> input) {
    TransferResult<AccumulationValue, AccumulationStore> res = super.visitLessThan(node, input);
    BinaryTree tree = node.getTree();
    assert (tree.getKind() == Tree.Kind.LESS_THAN)
        : "failed assumption: binaryTree in calledmethodsonelements transfer function is not lessthan tree";
    String calledMethod =
        MustCallOnElementsAnnotatedTypeFactory.whichMethodDoesLoopWithThisConditionCall(tree);
    if (calledMethod != null) {
      ExpressionTree arrayTree =
          MustCallOnElementsAnnotatedTypeFactory.getArrayTreeForLoopWithThisCondition(
              node.getTree());
      accumulate(arrayTree, res, calledMethod);
      return new ConditionalTransferResult<>(
          res.getResultValue(), res.getThenStore(), res.getElseStore());
    }
    return res;
  }

  public void accumulate(
      ExpressionTree tree,
      TransferResult<AccumulationValue, AccumulationStore> result,
      String... values) {
    List<String> valuesAsList = Arrays.asList(values);
    JavaExpression target = JavaExpression.fromTree(tree);
    System.out.println("oldtype: " + result.getElseStore().getValue(target));
    if (CFAbstractStore.canInsertJavaExpression(target)) {
      if (result.containsTwoStores()) {
        updateValueAndInsertIntoStore(result.getThenStore(), target, valuesAsList);
        updateValueAndInsertIntoStore(result.getElseStore(), target, valuesAsList);
      } else {
        updateValueAndInsertIntoStore(result.getRegularStore(), target, valuesAsList);
      }
    }
    System.out.println("newtype: " + result.getElseStore().getValue(target));
  }

  private void updateValueAndInsertIntoStore(
      AccumulationStore store, JavaExpression target, List<String> values) {
    // Make a modifiable copy of the list.
    List<String> valuesAsList = new ArrayList<>(values);
    AccumulationValue flowValue = store.getValue(target);
    if (flowValue != null) {
      Set<String> accumulatedValues = flowValue.getAccumulatedValues();
      if (accumulatedValues != null) {
        valuesAsList = CollectionsPlume.concatenate(valuesAsList, accumulatedValues);
      } else {
        AnnotationMirrorSet flowAnnos = flowValue.getAnnotations();
        assert flowAnnos.size() <= 1;
        for (AnnotationMirror anno : flowAnnos) {
          if (atypeFactory.isAccumulatorAnnotation(anno)) {
            List<String> oldFlowValues = atypeFactory.getAccumulatedValues(anno);
            if (!oldFlowValues.isEmpty()) {
              // valuesAsList cannot have its length changed -- it is backed by an
              // array -- but if oldFlowValues is not empty, it is a new, modifiable
              // list.
              oldFlowValues.addAll(valuesAsList);
              valuesAsList = oldFlowValues;
            }
          }
        }
      }
    }
    AnnotationMirror newAnno = atypeFactory.createAccumulatorAnnotation(valuesAsList);
    store.insertValue(target, newAnno);
  }

  //   ExecutableElement method = TreeUtils.elementFromUse(node.getTree());
  //   Node receiver = node.getTarget().getReceiver();
  //   if (receiver != null) {
  //     String methodName = node.getTarget().getMethod().getSimpleName().toString();
  //     methodName =
  //         ((CalledMethodsAnnotatedTypeFactory) atypeFactory)
  //             .adjustMethodNameUsingValueChecker(methodName, node.getTree());
  //     accumulate(receiver, superResult, methodName);
  //   }
  //   TransferResult<AccumulationValue, AccumulationStore> finalResult =
  //       new ConditionalTransferResult<>(
  //           superResult.getResultValue(),
  //           superResult.getThenStore(),
  //           superResult.getElseStore(),
  //           exceptionalStores);
  //   exceptionalStores = null;
  //   return finalResult;
  // }

  /**
   * Extract the current called-methods type from {@code currentType}, and then add each element of
   * {@code methodNames} to it, and return the result. This method is similar to GLB, but should be
   * used when the new methods come from a source other than an {@code CalledMethodsOnElements}
   * annotation.
   *
   * @param currentType the current type in the called-methods hierarchy
   * @param methodNames the names of the new methods to add to the type
   * @return the new annotation to be added to the type, or null if the current type cannot be
   *     converted to an accumulator annotation
   */
  private @Nullable AnnotationMirror getUpdatedCalledMethodsOnElementsType(
      AnnotatedTypeMirror currentType, String methodName) {
    AnnotationMirror type;
    if (currentType == null || !currentType.hasPrimaryAnnotationInHierarchy(atypeFactory.top)) {
      type = atypeFactory.top;
    } else {
      type = currentType.getPrimaryAnnotationInHierarchy(atypeFactory.top);
    }

    if (AnnotationUtils.areSame(type, atypeFactory.bottom)) {
      return null;
    }

    List<String> currentMethods =
        AnnotationUtils.getElementValueArray(
            type, calledMethodsOnElementsValueElement, String.class);

    List<String> newList = CollectionsPlume.append(currentMethods, methodName);

    return atypeFactory.createAccumulatorAnnotation(newList);
  }

  /**
   * Checks if WPI is enabled for the Resource Leak Checker inference. See {@link
   * ResourceLeakChecker#ENABLE_WPI_FOR_RLC}.
   *
   * @return returns true if WPI is enabled for the Resource Leak Checker
   */
  protected boolean isWpiEnabledForRLC() {
    return enableWpiForRlc;
  }
}
