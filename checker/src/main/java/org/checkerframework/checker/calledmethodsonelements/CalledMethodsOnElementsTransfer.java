package org.checkerframework.checker.calledmethodsonelements;

import com.sun.source.tree.ExpressionTree;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.mustcall.MustCallAnnotatedTypeFactory;
import org.checkerframework.checker.mustcall.qual.InheritableMustCall;
import org.checkerframework.checker.mustcall.qual.MustCall;
import org.checkerframework.checker.mustcallonelements.MustCallOnElementsAnnotatedTypeFactory;
import org.checkerframework.checker.mustcallonelements.qual.MustCallOnElements;
import org.checkerframework.checker.mustcallonelements.qual.MustCallOnElementsUnknown;
import org.checkerframework.checker.mustcallonelements.qual.OwningArray;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.resourceleak.ResourceLeakChecker;
import org.checkerframework.dataflow.analysis.ConditionalTransferResult;
import org.checkerframework.dataflow.analysis.RegularTransferResult;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.ArrayAccessNode;
import org.checkerframework.dataflow.cfg.node.LessThanNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.framework.flow.CFAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFTransfer;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TypesUtils;
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

  /** The processing environment. */
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

  /*
   * Empties the @MustCallOnElements() type of arguments passed as @OwningArray parameters to the
   * constructor and enforces that only @OwningArray arguments are passed to @OwningArray parameters.
   */
  // @Override
  // public TransferResult<CFValue, CFStore> visitObjectCreation(
  //     ObjectCreationNode node, TransferInput<CFValue, CFStore> input) {
  //   TransferResult<CFValue, CFStore> res = super.visitObjectCreation(node, input);
  //   ExecutableElement constructor = TreeUtils.elementFromUse(node.getTree());
  //   List<? extends VariableElement> params = constructor.getParameters();
  //   List<Node> args = node.getArguments();
  //   Iterator<? extends VariableElement> paramIterator = params.iterator();
  //   Iterator<Node> argIterator = args.iterator();
  //   while (paramIterator.hasNext() && argIterator.hasNext()) {
  //     VariableElement param = paramIterator.next();
  //     Node arg = argIterator.next();
  //     boolean paramIsOwningArray = param.getAnnotation(OwningArray.class) != null;
  //     if (paramIsOwningArray) {
  //       JavaExpression array = JavaExpression.fromNode(arg);
  //       AnnotationMirror oldType =
  // res.getRegularStore().getValue(array).getAnnotations().first();

  //       // extract the @MustCallOnElement values of the parameter
  //       List<String> mcoeObligationsOfComponent = Collections.emptyList();
  //       for (AnnotationMirror paramAnno : param.asType().getAnnotationMirrors()) {
  //         DeclaredType annotype = paramAnno.getAnnotationType();
  //         String annotypeQualifiedName =
  //             ElementUtils.getBinaryName((TypeElement) annotype.asElement()).toString();
  //         String mustCallOnElementsQualifiedName = MustCallOnElements.class.getCanonicalName();
  //         if (annotypeQualifiedName.equals(mustCallOnElementsQualifiedName)) {
  //           // is @MustCallOnElements annotation
  //           for (ExecutableElement key : paramAnno.getElementValues().keySet()) {
  //             AnnotationValue value = paramAnno.getElementValues().get(key);
  //             if ("value".equals(key.getSimpleName().toString())) {
  //               // Assuming the value is a list of strings (which it should be for a String array
  //               // annotation element)
  //               List<?> values = (List<?>) value.getValue();
  //               List<String> stringValues =
  //                   values.stream().map(Object::toString).collect(Collectors.toList());
  //               mcoeObligationsOfComponent =
  //                   CollectionsPlume.concatenate(mcoeObligationsOfComponent, stringValues);
  //             }
  //           }
  //         }
  //       }
  //       res.getRegularStore().clearValue(array);
  //       res.getRegularStore()
  //           .insertValue(
  //               array, getUpdatedCalledMethodsOnElementsType(oldType,
  // mcoeObligationsOfComponent));
  //     }
  //   }
  //   return res;
  // }
  //

  @Override
  public TransferResult<CFValue, CFStore> visitArrayAccess(
      ArrayAccessNode node, TransferInput<CFValue, CFStore> input) {
    // ExpressionTree arrayExpr = node.getArrayExpression();
    // System.out.println("arrayexpre: " + arrayExpr);
    return super.visitArrayAccess(node, input);
  }

  // /**
  //  * if the method invocation corresponds to the condition of a (desugared) enhanced for loop,
  //  * the method adds the methods called in that loop to the CMOE type of the corresponding array.
  //  *
  //  * @param node the method invocation node
  //  * @param input the transfer input
  //  */
  // private void updateStoreForEnhancedForLoop(
  //     LessThanNode node, TransferInput<CFValue, CFStore> input) {
  //   ExpressionTree iterableExpr = node.getIterableExpression();
  //   if (iterableExpr != null) {
  //     System.out.println("iterableexpr: " + node.getIterableExpression());
  //     System.out.println("            " + input);
  //   }
  // }

  /*
   * Empties the @MustCallOnElements type of arguments passed as @OwningArray parameters to the
   * method.
   */
  @Override
  public TransferResult<CFValue, CFStore> visitMethodInvocation(
      MethodInvocationNode node, TransferInput<CFValue, CFStore> input) {
    TransferResult<CFValue, CFStore> res = super.visitMethodInvocation(node, input);
    ExecutableElement method = node.getTarget().getMethod();
    List<? extends VariableElement> params = method.getParameters();
    List<Node> args = node.getArguments();
    Iterator<? extends VariableElement> paramIterator = params.iterator();
    Iterator<Node> argIterator = args.iterator();
    while (paramIterator.hasNext() && argIterator.hasNext()) {
      VariableElement param = paramIterator.next();
      Node arg = argIterator.next();
      boolean paramIsOwningArray = param.getAnnotation(OwningArray.class) != null;
      if (paramIsOwningArray) {
        JavaExpression array = JavaExpression.fromNode(arg);
        AnnotationMirror oldType = res.getRegularStore().getValue(array).getAnnotations().first();

        // extract the @MustCallOnElement values of the parameter
        List<String> mcoeObligationsOfComponent = Collections.emptyList();
        boolean paramHasMcoeAnno = false;
        for (AnnotationMirror paramAnno : param.asType().getAnnotationMirrors()) {
          if (AnnotationUtils.areSameByName(
              paramAnno, MustCallOnElements.class.getCanonicalName())) {
            // is @MustCallOnElements annotation
            paramHasMcoeAnno = true;
            for (ExecutableElement key : paramAnno.getElementValues().keySet()) {
              AnnotationValue value = paramAnno.getElementValues().get(key);
              if ("value".equals(key.getSimpleName().toString())) {
                // Assuming the value is a list of strings (which it should be for a String array
                // annotation element)
                List<?> values = (List<?>) value.getValue();
                List<String> stringValues =
                    values.stream().map(Object::toString).collect(Collectors.toList());
                mcoeObligationsOfComponent =
                    CollectionsPlume.concatenate(mcoeObligationsOfComponent, stringValues);
              }
            }
          }
          if (AnnotationUtils.areSameByName(
              paramAnno, MustCallOnElementsUnknown.class.getCanonicalName())) {
            // if mcoeUnknown annotation, no methods are guaranteed to be called
            paramHasMcoeAnno = true;
          }
        }
        if (!paramHasMcoeAnno) {
          // if no mcoe anno, the mcoe type defaults to all obligations of the component
          assert param.asType() instanceof ArrayType : "@OwningArray parameter is not arraytype";
          mcoeObligationsOfComponent =
              getMustCallValuesForType(((ArrayType) param.asType()).getComponentType());
        }
        CFStore store = res.getRegularStore();
        store.clearValue(array);
        store.insertValue(
            array, getUpdatedCalledMethodsOnElementsType(oldType, mcoeObligationsOfComponent));
        return new RegularTransferResult<CFValue, CFStore>(res.getResultValue(), store);
      }
    }
    return res;
  }

  /**
   * Returns the list of mustcall obligations for a type.
   *
   * @param type the type
   * @return the list of mustcall obligations for the type
   */
  private List<String> getMustCallValuesForType(TypeMirror type) {
    MustCallAnnotatedTypeFactory mcAtf =
        new MustCallAnnotatedTypeFactory(atypeFactory.getChecker());
    TypeElement typeElement = TypesUtils.getTypeElement(type);
    AnnotationMirror imcAnnotation =
        mcAtf.getDeclAnnotation(typeElement, InheritableMustCall.class);
    AnnotationMirror mcAnnotation = mcAtf.getDeclAnnotation(typeElement, MustCall.class);
    Set<String> mcValues = new HashSet<>();
    if (mcAnnotation != null) {
      mcValues.addAll(
          AnnotationUtils.getElementValueArray(
              mcAnnotation, mcAtf.getMustCallValueElement(), String.class));
    }
    if (imcAnnotation != null) {
      mcValues.addAll(
          AnnotationUtils.getElementValueArray(
              imcAnnotation, mcAtf.getInheritableMustCallValueElement(), String.class));
    }
    return new ArrayList<>(mcValues);
  }

  /**
   * Checks whether the passed {@code LessThanNode} is the condition of an allocating for loop (for
   * an {@code @OwningArray}), in which case the {@code @CalledMethodsOnElements} type of the
   * corresponding array is emptied in the else store of the {@code TransferResult}.
   *
   * @param node the {@code LessThanNode} that is possibly the condition of an allocating for loop
   * @param res the transfer result to update
   * @return the updates {@code TransferResult}
   */
  private TransferResult<CFValue, CFStore> updateTransferResultForAllocatingForLoop(
      LessThanNode node, TransferResult<CFValue, CFStore> res) {
    ExpressionTree arrayTree =
        MustCallOnElementsAnnotatedTypeFactory.getArrayTreeForLoopWithThisCondition(node.getTree());
    if (arrayTree == null) return res;
    JavaExpression target = JavaExpression.fromTree(arrayTree);
    List<String> mustCall =
        MustCallOnElementsAnnotatedTypeFactory.whichObligationsDoesLoopWithThisConditionCreate(
            node.getTree());
    if (mustCall != null) {
      // array is newly assigned -> remove all calledmethods
      AnnotationMirror newAnno =
          createAccumulatorAnnotation(Collections.emptyList(), atypeFactory.TOP);
      CFStore elseStore = res.getElseStore();
      elseStore.clearValue(target);
      elseStore.insertValue(target, newAnno);
      return new ConditionalTransferResult<>(res.getResultValue(), res.getThenStore(), elseStore);
    }
    return res;
  }

  /**
   * Checks whether the passed {@code LessThanNode} is the condition of an obligation-fulfilling for
   * loop (for an {@code @OwningArray}), in which case the methods called in the loop body are added
   * to the {@code @CalledMethodsOnElements} type of the corresponding array in the else store of
   * the {@code TransferResult}.
   *
   * @param node the {@code LessThanNode} that is possibly the condition of an obligation-fulfilling
   *     for-loop
   * @param res the transfer result to update
   * @return the updated transfer result
   */
  private TransferResult<CFValue, CFStore> updateTransferResultForFulfillingForLoop(
      LessThanNode node, TransferResult<CFValue, CFStore> res) {
    ExpressionTree arrayTree =
        MustCallOnElementsAnnotatedTypeFactory.getArrayTreeForLoopWithThisCondition(node.getTree());
    if (arrayTree == null) return res;
    Set<String> calledMethods =
        MustCallOnElementsAnnotatedTypeFactory.whichMethodsDoesLoopWithThisConditionCall(
            node.getTree());
    if (calledMethods != null && calledMethods.size() > 0) {
      CFStore elseStore = res.getElseStore();
      JavaExpression target = JavaExpression.fromTree(arrayTree);
      CFValue oldTypeValue = elseStore.getValue(target);
      assert oldTypeValue != null : "Array " + arrayTree + " not in Store.";
      AnnotationMirror oldType = oldTypeValue.getAnnotations().first();
      AnnotationMirror newType =
          getUpdatedCalledMethodsOnElementsType(oldType, new ArrayList<>(calledMethods));
      elseStore.clearValue(target);
      elseStore.insertValue(target, newType);
      return new ConditionalTransferResult<>(res.getResultValue(), res.getThenStore(), elseStore);
    }
    return res;
  }

  /**
   * update {@code @CalledMethodsOnElements} type for pattern-matched loop that has a LessThan as
   * its condition.
   */
  @Override
  public TransferResult<CFValue, CFStore> visitLessThan(
      LessThanNode node, TransferInput<CFValue, CFStore> input) {
    TransferResult<CFValue, CFStore> res = super.visitLessThan(node, input);
    // if (TreeUtils.statementIsSynthetic(node.getTree())) {
    //   BinaryTree lessThanTree = node.getTree();
    //   System.out.println("ltTree: " + lessThanTree);
    //     Node rhs = node.getRightOperand();
    //     System.out.println("rhs: " + rhs);
    //     if (rhs instanceof FieldAccessNode) {
    //       FieldAccessNode accessNode = (FieldAccessNode) rhs;
    //       if (accessNode.getFieldName().equals("length")) {
    //         MustCallAnnotatedTypeFactory mcatf =
    //             new MustCallAnnotatedTypeFactory(atypeFactory.getChecker());
    //         Node collectionNode = accessNode.getReceiver();
    //         System.out.println("receiver: " + collectionNode);
    //         if (mcatf.getDeclAnnotation(
    //                 TreeUtils.elementFromTree(collectionNode.getTree()), OwningArray.class)
    //             != null) {
    //           System.out.println("collectionNode: " + accessNode.getFieldName());
    //         }
    //       }
    //     }
    // }

    res = updateTransferResultForAllocatingForLoop(node, res);
    return updateTransferResultForFulfillingForLoop(node, res);

    // return res;
  }

  /**
   * Extract the current called-methods type from {@code currentType}, and then add {@code
   * methodName} to it, and return the result. This method is similar to GLB, but should be used
   * when the new methods come from a source other than an {@code CalledMethodsOnElements}
   * annotation.
   *
   * @param type the current type in the called-methods hierarchy
   * @param methodNames list of names of the new methods to add to the type
   * @return the new annotation to be added to the type, or null if the current type cannot be
   *     converted to an accumulator annotation
   */
  private @Nullable AnnotationMirror getUpdatedCalledMethodsOnElementsType(
      AnnotationMirror type, List<String> methodNames) {
    List<String> currentMethods =
        AnnotationUtils.getElementValueArray(
            type, calledMethodsOnElementsValueElement, String.class);
    List<String> newList = CollectionsPlume.concatenate(currentMethods, methodNames);
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
   * @param type the {@code AnnotationMirror} to build upon
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
