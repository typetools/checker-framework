package org.checkerframework.checker.mustcallonelements;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;
import java.util.List;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.checker.mustcall.MustCallAnnotatedTypeFactory;
import org.checkerframework.checker.mustcall.qual.*;
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

  // @Override
  // public TransferResult<CFValue, CFStore> visitVariableDeclaration(
  //     VariableDeclarationNode node, TransferInput<CFValue, CFStore> input) {
  //   TransferResult<CFValue, CFStore> res = super.visitVariableDeclaration(node, input);
  //   // since @OwningArray is enforced to be array, the following cast is guaranteed to succeed
  //   VariableElement elmnt = TreeUtils.elementFromDeclaration(node.getTree());
  //   if (atypeFactory.getDeclAnnotation(elmnt, OwningArray.class) != null
  //       && elmnt.getKind() == ElementKind.FIELD) {
  //     TypeMirror componentType = ((ArrayType) elmnt.asType()).getComponentType();
  //     List<String> mcoeObligationsOfOwningField = getMustCallValuesForType(componentType);
  //     AnnotationMirror newType = getMustCallOnElementsType(mcoeObligationsOfOwningField);
  //     JavaExpression field = JavaExpression.fromVariableTree(node.getTree());
  //     res.getRegularStore().clearValue(field);
  //     res.getRegularStore().insertValue(field, newType);
  //   }
  //   return res;
  // }

  // /**
  //  * Returns the list of mustcall obligations for a type.
  //  *
  //  * @param type the type
  //  * @return the list of mustcall obligations for the type
  //  */
  // private List<String> getMustCallValuesForType(TypeMirror type) {
  //   InheritableMustCall imcAnnotation =
  //       TypesUtils.getClassFromType(type).getAnnotation(InheritableMustCall.class);
  //   MustCall mcAnnotation = TypesUtils.getClassFromType(type).getAnnotation(MustCall.class);
  //   Set<String> mcValues = new HashSet<>();
  //   if (mcAnnotation != null) {
  //     mcValues.addAll(Arrays.asList(mcAnnotation.value()));
  //   }
  //   if (imcAnnotation != null) {
  //     mcValues.addAll(Arrays.asList(imcAnnotation.value()));
  //   }
  //   return new ArrayList<>(mcValues);
  // }

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
    String calledMethod =
        MustCallOnElementsAnnotatedTypeFactory.whichMethodDoesLoopWithThisConditionCall(tree);
    ExpressionTree arrayTree =
        MustCallOnElementsAnnotatedTypeFactory.getArrayTreeForLoopWithThisCondition(tree);
    CFStore elseStore = res.getElseStore();
    JavaExpression receiverReceiver = JavaExpression.fromTree(arrayTree);
    if (newMustCallMethods != null) {
      // this is an obligation-creating loop
      AnnotationMirror newType = getMustCallOnElementsType(newMustCallMethods);
      elseStore.clearValue(receiverReceiver);
      elseStore.insertValue(receiverReceiver, newType);
      return new ConditionalTransferResult<>(res.getResultValue(), res.getThenStore(), elseStore);
    } else if (calledMethod != null) {
      // this loop fulfills an obligation - remove that methodname from
      // the MustCallOnElements type of the array
      CFValue oldTypeValue = elseStore.getValue(receiverReceiver);
      assert oldTypeValue != null : "Array " + arrayTree + " not in Store.";
      AnnotationMirror oldType = oldTypeValue.getAnnotations().first();
      List<String> mcoeMethods =
          AnnotationUtils.getElementValueArray(
              oldType, atypeFactory.getMustCallOnElementsValueElement(), String.class);
      mcoeMethods.remove(calledMethod);
      AnnotationMirror newType = getMustCallOnElementsType(mcoeMethods);
      elseStore.clearValue(receiverReceiver);
      elseStore.insertValue(receiverReceiver, newType);
      return new ConditionalTransferResult<>(res.getResultValue(), res.getThenStore(), elseStore);
    }
    return res;
  }

  /**
   * Generate an annotation from a list of method names.
   *
   * @param methodNames the names of the methods to add to the type
   * @return the annotation with the given methods as value
   */
  private @Nullable AnnotationMirror getMustCallOnElementsType(List<String> methodNames) {
    AnnotationBuilder builder = new AnnotationBuilder(this.env, atypeFactory.BOTTOM);
    builder.setValue("value", CollectionsPlume.withoutDuplicatesSorted(methodNames));
    return builder.build();
  }

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

  // /**
  //  * Pretty-prints a list of mustcall values into a string to output in a warning message.
  //  *
  //  * @param mustCallVal a list of mustcall values
  //  * @return a string, which is a pretty-print of the method list
  //  */
  // private String formatMissingMustCallMethods(List<String> mustCallVal) {
  //   int size = mustCallVal.size();
  //   if (size == 0) {
  //     return "None";
  //   } else if (size == 1) {
  //     return "method " + mustCallVal.get(0);
  //   } else {
  //     return "methods " + String.join(", ", mustCallVal);
  //   }
  // }
}
