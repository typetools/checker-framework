package org.checkerframework.checker.calledmethodsonelements;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;
import java.util.Collections;
import java.util.List;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import org.checkerframework.checker.mustcallonelements.MustCallOnElementsAnnotatedTypeFactory;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.resourceleak.ResourceLeakChecker;
import org.checkerframework.dataflow.analysis.ConditionalTransferResult;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.LessThanNode;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.framework.flow.CFAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFTransfer;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.type.*;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;
import org.plumelib.util.CollectionsPlume;

/** A transfer function that accumulates the names of methods called. */
public class CalledMethodsOnElementsTransfer extends CFTransfer {
  /**
   * The element for the CalledMethodsOnElements annotation's value element. Stored in a field in
   * this class to prevent the need to cast to CalledMethodsOnElements ATF every time it's used.
   */
  private final ExecutableElement calledMethodsOnElementsValueElement;

  /** The type factory. */
  private final CalledMethodsOnElementsAnnotatedTypeFactory atypeFactory;

  private final ProcessingEnvironment env;

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
  public CalledMethodsOnElementsTransfer(CFAnalysis analysis) {
    super(analysis);
    if (analysis.getTypeFactory() instanceof CalledMethodsOnElementsAnnotatedTypeFactory) {
      atypeFactory = (CalledMethodsOnElementsAnnotatedTypeFactory) analysis.getTypeFactory();
    } else {
      atypeFactory =
          new CalledMethodsOnElementsAnnotatedTypeFactory(analysis.getTypeFactory().getChecker());
    }

    calledMethodsOnElementsValueElement = atypeFactory.calledMethodsOnElementsValueElement;
    enableWpiForRlc = atypeFactory.getChecker().hasOption(ResourceLeakChecker.ENABLE_WPI_FOR_RLC);
    env = atypeFactory.getProcessingEnv();
  }

  /**
   * {@inheritDoc}
   *
   * <p>Furthermore, this method refines the type to {@code NonNull} for the appropriate branch if
   * an expression is compared to the {@code null} literal (listed as case 1 in the class
   * description).
   */
  @Override
  public TransferResult<CFValue, CFStore> visitLessThan(
      LessThanNode node, TransferInput<CFValue, CFStore> input) {
    TransferResult<CFValue, CFStore> res = super.visitLessThan(node, input);
    BinaryTree tree = node.getTree();
    assert (tree.getKind() == Tree.Kind.LESS_THAN)
        : "failed assumption: binaryTree in calledmethodsonelements transfer function is not"
            + " lessthan tree";
    String calledMethod =
        MustCallOnElementsAnnotatedTypeFactory.whichMethodDoesLoopWithThisConditionCall(tree);
    ExpressionTree arrayTree =
        MustCallOnElementsAnnotatedTypeFactory.getArrayTreeForLoopWithThisCondition(node.getTree());
    JavaExpression target = JavaExpression.fromTree(arrayTree);
    CFStore elseStore = res.getElseStore();
    if (calledMethod != null) {
      CFValue oldTypeValue = elseStore.getValue(target);
      assert oldTypeValue != null : "Array " + arrayTree + " not in Store.";
      AnnotationMirror oldType = oldTypeValue.getAnnotations().first();
      AnnotationMirror newType = getUpdatedCalledMethodsOnElementsType(oldType, calledMethod);
      elseStore.clearValue(target);
      elseStore.insertValue(target, newType);
      return new ConditionalTransferResult<>(res.getResultValue(), res.getThenStore(), elseStore);
    }
    List<String> mustCall =
        MustCallOnElementsAnnotatedTypeFactory.whichObligationsDoesLoopWithThisConditionCreate(
            tree);
    if (mustCall != null) {
      // array is newly assigned -> remove all calledmethods
      AnnotationMirror newAnno =
          createAccumulatorAnnotation(Collections.emptyList(), atypeFactory.TOP);
      elseStore.clearValue(target);
      elseStore.insertValue(target, newAnno);
      return new ConditionalTransferResult<>(res.getResultValue(), res.getThenStore(), elseStore);
    }

    return res;
  }

  /**
   * Extract the current called-methods type from {@code currentType}, and then add {@code
   * methodName} to it, and return the result. This method is similar to GLB, but should be used
   * when the new methods come from a source other than an {@code CalledMethodsOnElements}
   * annotation.
   *
   * @param type the current type in the called-methods hierarchy
   * @param methodName the name of the new method to add to the type
   * @return the new annotation to be added to the type, or null if the current type cannot be
   *     converted to an accumulator annotation
   */
  private @Nullable AnnotationMirror getUpdatedCalledMethodsOnElementsType(
      AnnotationMirror type, String methodName) {
    List<String> currentMethods =
        AnnotationUtils.getElementValueArray(
            type, calledMethodsOnElementsValueElement, String.class);
    List<String> newList = CollectionsPlume.append(currentMethods, methodName);
    return createAccumulatorAnnotation(newList, type);
  }

  /**
   * Creates a new instance of the accumulator annotation that contains the elements of {@code
   * values}.
   *
   * @param values the arguments to the annotation. The values can contain duplicates and can be in
   *     any order.
   * @return an annotation mirror representing the accumulator annotation with {@code values}'s
   *     arguments; this is top if {@code values} is empty
   */
  public AnnotationMirror createAccumulatorAnnotation(List<String> values, AnnotationMirror type) {
    AnnotationBuilder builder = new AnnotationBuilder(this.env, type);
    builder.setValue("value", CollectionsPlume.withoutDuplicatesSorted(values));
    return builder.build();
  }

  /**
   * Creates a new instance of the accumulator annotation that contains exactly one value.
   *
   * @param value the argument to the annotation
   * @return an annotation mirror representing the accumulator annotation with {@code value} as its
   *     argument
   */
  public AnnotationMirror createAccumulatorAnnotation(String value, AnnotationMirror type) {
    AnnotationBuilder builder = new AnnotationBuilder(this.env, type);
    builder.setValue("value", Collections.singletonList(value));
    return builder.build();
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
