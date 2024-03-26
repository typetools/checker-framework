package org.checkerframework.checker.mustcallonelements;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import org.checkerframework.checker.mustcall.MustCallAnnotatedTypeFactory;
import org.checkerframework.checker.mustcallonelements.qual.MustCallOnElements;
import org.checkerframework.checker.mustcallonelements.qual.MustCallOnElementsUnknown;
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
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TreeUtils;
import org.plumelib.util.CollectionsPlume;

/** Transfer function for the MustCallOnElements type system. */
public class MustCallOnElementsTransfer extends CFTransfer {

  /** The type factory. */
  private final MustCallOnElementsAnnotatedTypeFactory atypeFactory;

  /** True if -AenableWpiForRlc was passed on the command line. */
  private final boolean enableWpiForRlc;

  private final ProcessingEnvironment env;

  /**
   * Create a MustCallOnElementsTransfer.
   *
   * @param analysis the analysis
   */
  public MustCallOnElementsTransfer(CFAnalysis analysis) {
    super(analysis);
    if (analysis.getTypeFactory() instanceof MustCallOnElementsAnnotatedTypeFactory) {
      atypeFactory = (MustCallOnElementsAnnotatedTypeFactory) analysis.getTypeFactory();
    } else {
      atypeFactory =
          new MustCallOnElementsAnnotatedTypeFactory(
              ((MustCallAnnotatedTypeFactory) analysis.getTypeFactory()).getChecker());
    }
    enableWpiForRlc = atypeFactory.getChecker().hasOption(ResourceLeakChecker.ENABLE_WPI_FOR_RLC);
    this.env = atypeFactory.getChecker().getProcessingEnvironment();
  }

  @Override
  public TransferResult<CFValue, CFStore> visitLessThan(
      LessThanNode node, TransferInput<CFValue, CFStore> input) {
    TransferResult<CFValue, CFStore> res = super.visitLessThan(node, input);
    BinaryTree tree = node.getTree();
    assert (tree.getKind() == Tree.Kind.LESS_THAN)
        : "failed assumption: binaryTree in calledmethodsonelements transfer function is not lessthan tree";
    List<String> newMustCallMethods =
        MustCallOnElementsAnnotatedTypeFactory.whichObligationsDoesLoopWithThisConditionCreate(
            tree);
    if (newMustCallMethods == null) {
      // it is not an obligation-creating loop
      return res;
    }
    ExpressionTree arrayTree =
        MustCallOnElementsAnnotatedTypeFactory.getArrayTreeForLoopWithThisCondition(tree);
    Element arrayElt = TreeUtils.elementFromTree(arrayTree);
    if (arrayElt.getKind() == ElementKind.FIELD) {
      List<String> previousMustCallMethods =
          MustCallOnElementsAnnotatedTypeFactory.getMcoeObligationsForOwningArrayField(
              arrayTree.toString());
      if (previousMustCallMethods.isEmpty()) {
        MustCallOnElementsAnnotatedTypeFactory.putMcoeObligationsForOwningArrayField(
            arrayTree.toString(), newMustCallMethods);
      } else {
        boolean areObligationsEqual =
            new HashSet<String>(previousMustCallMethods)
                .equals(new HashSet<String>(newMustCallMethods));
        if (!areObligationsEqual) {
          atypeFactory
              .getChecker()
              .reportError(
                  arrayTree,
                  "wrong.mustCallOnElements.obligations.assigned",
                  arrayTree.toString(),
                  formatMissingMustCallMethods(previousMustCallMethods),
                  formatMissingMustCallMethods(newMustCallMethods));
          return res;
        }
      }
    }
    CFStore elseStore = res.getElseStore();
    // AnnotatedTypeMirror currentType = atypeFactory.getAnnotatedType(arrayTree);
    AnnotationMirror newType = getMustCallOnElementsType(newMustCallMethods);
    JavaExpression receiverReceiver = JavaExpression.fromTree(arrayTree);
    elseStore.clearValue(receiverReceiver);
    elseStore.insertValue(receiverReceiver, newType);
    return new ConditionalTransferResult<>(res.getResultValue(), res.getThenStore(), elseStore);
  }

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
  private @Nullable AnnotationMirror getMustCallOnElementsType(List<String> methodNames) {
    AnnotationBuilder builder = new AnnotationBuilder(this.env, atypeFactory.BOTTOM);
    builder.setValue("value", CollectionsPlume.withoutDuplicatesSorted(methodNames));
    return builder.build();
  }

  // /**
  //  * Creates a new instance of the accumulator annotation that contains the elements of {@code
  //  * values}.
  //  *
  //  * @param values the arguments to the annotation. The values can contain duplicates and can be in
  //  *     any order.
  //  * @return an annotation mirror representing the accumulator annotation with {@code values}'s
  //  *     arguments; this is top if {@code values} is empty
  //  */
  // public AnnotationMirror createAccumulatorAnnotation(List<String> values, AnnotationMirror type) {
  //   AnnotationBuilder builder = new AnnotationBuilder(this.env, type);
  //   builder.setValue("value", CollectionsPlume.withoutDuplicatesSorted(values));
  //   return builder.build();
  // }

  // /**
  //  * Creates a new instance of the accumulator annotation that contains exactly one value.
  //  *
  //  * @param value the argument to the annotation
  //  * @return an annotation mirror representing the accumulator annotation with {@code value} as its
  //  *     argument
  //  */
  // public AnnotationMirror createAccumulatorAnnotation(String value, AnnotationMirror type) {
  //   AnnotationBuilder builder = new AnnotationBuilder(this.env, type);
  //   builder.setValue("value", Collections.singletonList(value));
  //   return builder.build();
  // }

  /**
   * @param tree a tree
   * @return false if Resource Leak Checker is running as one of the upstream checkers and the
   *     -AenableWpiForRlc flag is not passed as a command line argument, otherwise returns the
   *     result of the super call
   */
  @Override
  protected boolean shouldPerformWholeProgramInference(Tree tree) {
    if (!isWpiEnabledForRLC()
        && atypeFactory.getCheckerNames().contains(ResourceLeakChecker.class.getCanonicalName())) {
      return false;
    }
    return super.shouldPerformWholeProgramInference(tree);
  }

  /**
   * @param expressionTree a tree
   * @param lhsTree its element
   * @return false if Resource Leak Checker is running as one of the upstream checkers and the
   *     -AenableWpiForRlc flag is not passed as a command line argument, otherwise returns the
   *     result of the super call
   */
  @Override
  protected boolean shouldPerformWholeProgramInference(Tree expressionTree, Tree lhsTree) {
    if (!isWpiEnabledForRLC()
        && atypeFactory.getCheckerNames().contains(ResourceLeakChecker.class.getCanonicalName())) {
      return false;
    }
    return super.shouldPerformWholeProgramInference(expressionTree, lhsTree);
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

  /**
   * Pretty-prints a list of mustcall values into a string to output in a warning message.
   *
   * @param mustCallVal a list of mustcall values
   * @return a string, which is a pretty-print of the method list
   */
  private String formatMissingMustCallMethods(List<String> mustCallVal) {
    int size = mustCallVal.size();
    if (size == 0) {
      return "None";
    } else if (size == 1) {
      return "method " + mustCallVal.get(0);
    } else {
      return "methods " + String.join(", ", mustCallVal);
    }
  }
}
