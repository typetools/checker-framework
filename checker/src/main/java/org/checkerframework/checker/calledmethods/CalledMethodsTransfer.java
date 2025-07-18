package org.checkerframework.checker.calledmethods;

import com.sun.source.tree.Tree;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import org.checkerframework.checker.calledmethods.qual.EnsuresCalledMethodsVarargs;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.resourceleak.ResourceLeakChecker;
import org.checkerframework.common.accumulation.AccumulationStore;
import org.checkerframework.common.accumulation.AccumulationTransfer;
import org.checkerframework.common.accumulation.AccumulationValue;
import org.checkerframework.dataflow.analysis.ConditionalTransferResult;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.block.ExceptionBlock;
import org.checkerframework.dataflow.cfg.node.ArrayCreationNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.framework.flow.CFAbstractStore;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.util.JavaExpressionParseUtil;
import org.checkerframework.framework.util.StringToJavaExpression;
import org.checkerframework.javacutil.AnnotationMirrorSet;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;
import org.plumelib.util.CollectionsPlume;

/** A transfer function that accumulates the names of methods called. */
public class CalledMethodsTransfer extends AccumulationTransfer {

  /**
   * The element for the CalledMethods annotation's value element. Stored in a field in this class
   * to prevent the need to cast to CalledMethods ATF every time it's used.
   */
  private final ExecutableElement calledMethodsValueElement;

  /** The type mirror for {@link Exception}. */
  protected final TypeMirror javaLangExceptionType;

  /**
   * True if -AenableWpiForRlc was passed on the command line. See {@link
   * ResourceLeakChecker#ENABLE_WPI_FOR_RLC}.
   */
  private final boolean enableWpiForRlc;

  /**
   * Create a new CalledMethodsTransfer.
   *
   * @param analysis the analysis
   */
  public CalledMethodsTransfer(CalledMethodsAnalysis analysis) {
    super(analysis);
    calledMethodsValueElement =
        ((CalledMethodsAnnotatedTypeFactory) atypeFactory).calledMethodsValueElement;
    enableWpiForRlc = atypeFactory.getChecker().hasOption(ResourceLeakChecker.ENABLE_WPI_FOR_RLC);

    ProcessingEnvironment env = atypeFactory.getProcessingEnv();
    javaLangExceptionType =
        env.getTypeUtils().getDeclaredType(ElementUtils.getTypeElement(env, Exception.class));
  }

  /**
   * @param tree a tree
   * @return false if Resource Leak Checker is running as one of the upstream checkers and the
   *     -AenableWpiForRlc flag (see {@link ResourceLeakChecker#ENABLE_WPI_FOR_RLC}) is not passed
   *     as a command line argument, otherwise returns the result of the super call
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
   * See {@link ResourceLeakChecker#ENABLE_WPI_FOR_RLC}.
   *
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

  @Override
  public TransferResult<AccumulationValue, AccumulationStore> visitMethodInvocation(
      MethodInvocationNode node, TransferInput<AccumulationValue, AccumulationStore> input) {

    // The call to `super.visitMethodInvocation()` modifies the input store in-place.  So if we end
    // up needing to create the exceptional stores, then we'll need this copy taken beforehand.
    AccumulationStore inputStore = input.getRegularStore().copy();

    TransferResult<AccumulationValue, AccumulationStore> superResult =
        super.visitMethodInvocation(node, input);

    // Ensure that the result has a store for each possible exception.  This affects the behavior of
    // accumulate(), which will accumulate values into the result's exceptional stores as well.
    Map<TypeMirror, AccumulationStore> exceptionalStores = superResult.getExceptionalStores();
    if (exceptionalStores == null) {
      exceptionalStores = makeExceptionalStores(node, inputStore);
      superResult = superResult.withExceptionalStores(exceptionalStores);
    }

    ExecutableElement method = TreeUtils.elementFromUse(node.getTree());
    handleEnsuresCalledMethodsVarargs(node, method, superResult);
    handleEnsuresCalledMethodsOnException(node, method, exceptionalStores);

    Node receiver = node.getTarget().getReceiver();
    if (receiver != null) {
      String methodName = node.getTarget().getMethod().getSimpleName().toString();
      methodName =
          ((CalledMethodsAnnotatedTypeFactory) atypeFactory)
              .adjustMethodNameUsingValueChecker(methodName, node.getTree());
      accumulate(receiver, superResult, methodName);
    }
    return new ConditionalTransferResult<>(
        superResult.getResultValue(),
        superResult.getThenStore(),
        superResult.getElseStore(),
        exceptionalStores);
  }

  @Override
  public void accumulate(
      Node node, TransferResult<AccumulationValue, AccumulationStore> result, String... values) {
    super.accumulate(node, result, values);

    Map<TypeMirror, AccumulationStore> exceptionalStores = result.getExceptionalStores();
    if (exceptionalStores == null) {
      return;
    }

    List<String> valuesAsList = Arrays.asList(values);
    JavaExpression target = JavaExpression.fromNode(node);
    if (CFAbstractStore.canInsertJavaExpression(target)) {
      AccumulationValue flowValue = result.getRegularStore().getValue(target);
      if (flowValue != null) {
        // Dataflow has already recorded information about the target.  Integrate it into
        // the list of values in the new annotation.
        AnnotationMirrorSet flowAnnos = flowValue.getAnnotations();
        assert flowAnnos.size() <= 1;
        for (AnnotationMirror anno : flowAnnos) {
          if (atypeFactory.isAccumulatorAnnotation(anno)) {
            List<String> oldFlowValues =
                AnnotationUtils.getElementValueArray(anno, calledMethodsValueElement, String.class);
            // valuesAsList cannot have its length changed -- it is backed by an
            // array.  getElementValueArray returns a new, modifiable list.
            oldFlowValues.addAll(valuesAsList);
            valuesAsList = oldFlowValues;
          }
        }
      }
      AnnotationMirror newAnno = atypeFactory.createAccumulatorAnnotation(valuesAsList);
      exceptionalStores.forEach(
          (tm, s) ->
              s.replaceValue(
                  target, analysis.createSingleAnnotationValue(newAnno, target.getType())));
    }
  }

  /**
   * Create a set of stores for the exceptional paths out of the block containing {@code node}. This
   * allows propagation, along those paths, of the fact that the method being invoked in {@code
   * node} was definitely called.
   *
   * @param node a method invocation
   * @param inputStore the transfer input associated with the method invocation
   * @return a map from types to stores. The keys are the same keys used by {@link
   *     ExceptionBlock#getExceptionalSuccessors()}. The values are copies of the regular store from
   *     {@code input}.
   */
  private Map<TypeMirror, AccumulationStore> makeExceptionalStores(
      MethodInvocationNode node, AccumulationStore inputStore) {
    if (!(node.getBlock() instanceof ExceptionBlock)) {
      // This can happen in some weird (buggy?) cases:
      // see https://github.com/typetools/checker-framework/issues/3585
      return Collections.emptyMap();
    }
    ExceptionBlock block = (ExceptionBlock) node.getBlock();
    Map<TypeMirror, AccumulationStore> result = new LinkedHashMap<>();
    block.getExceptionalSuccessors().forEach((tm, b) -> result.put(tm, inputStore.copy()));
    return result;
  }

  /**
   * Update the types of varargs parameters passed to a method with an {@link
   * EnsuresCalledMethodsVarargs} annotation. This method is a no-op if no such annotation is
   * present.
   *
   * @param node the method invocation node
   * @param elt the method being invoked
   * @param result the current result
   */
  @SuppressWarnings("deprecation") // EnsuresCalledMethodsVarArgs
  private void handleEnsuresCalledMethodsVarargs(
      MethodInvocationNode node,
      ExecutableElement elt,
      TransferResult<AccumulationValue, AccumulationStore> result) {
    AnnotationMirror annot = atypeFactory.getDeclAnnotation(elt, EnsuresCalledMethodsVarargs.class);
    if (annot == null) {
      return;
    }
    List<String> ensuredMethodNames =
        AnnotationUtils.getElementValueArray(
            annot,
            ((CalledMethodsAnnotatedTypeFactory) atypeFactory)
                .ensuresCalledMethodsVarargsValueElement,
            String.class);
    List<? extends VariableElement> parameters = elt.getParameters();
    int varArgsPos = parameters.size() - 1;
    Node varArgActual = node.getArguments().get(varArgsPos);
    // In the CFG, explicit passing of multiple arguments in the varargs position is represented
    // via an ArrayCreationNode.  This is the only case we handle for now.
    if (varArgActual instanceof ArrayCreationNode) {
      ArrayCreationNode arrayCreationNode = (ArrayCreationNode) varArgActual;
      // add in the called method to all the vararg arguments
      AccumulationStore thenStore = result.getThenStore();
      AccumulationStore elseStore = result.getElseStore();
      for (Node arg : arrayCreationNode.getInitializers()) {
        AnnotatedTypeMirror currentType = atypeFactory.getAnnotatedType(arg.getTree());
        AnnotationMirror newType = getUpdatedCalledMethodsType(currentType, ensuredMethodNames);
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
   * Update the given <code>exceptionalStores</code> for the {@link
   * org.checkerframework.checker.calledmethods.qual.EnsuresCalledMethodsOnException} annotations
   * written on the given <code>method</code>.
   *
   * @param node a method invocation
   * @param method the method being invoked
   * @param exceptionalStores the stores to update
   */
  private void handleEnsuresCalledMethodsOnException(
      MethodInvocationNode node,
      ExecutableElement method,
      Map<TypeMirror, AccumulationStore> exceptionalStores) {
    Types types = atypeFactory.getProcessingEnv().getTypeUtils();
    for (EnsuresCalledMethodOnExceptionContract postcond :
        ((CalledMethodsAnnotatedTypeFactory) atypeFactory).getExceptionalPostconditions(method)) {
      JavaExpression e;
      try {
        e =
            StringToJavaExpression.atMethodInvocation(
                postcond.getExpression(), node.getTree(), atypeFactory.getChecker());
      } catch (JavaExpressionParseUtil.JavaExpressionParseException ex) {
        // This parse error will be reported later. For now, we'll skip this malformed
        // postcondition and move on to the others.
        continue;
      }

      // NOTE: this code is a little inefficient; it creates a single-method annotation and
      // calls `insertOrRefine` in a loop.  Even worse, this code appears within a loop.
      // For now we aren't too worried about it, since the number of
      // EnsuresCalledMethodsOnException annotations should be small.
      AnnotationMirror calledMethod =
          atypeFactory.createAccumulatorAnnotation(postcond.getMethod());
      for (Map.Entry<TypeMirror, AccumulationStore> successor : exceptionalStores.entrySet()) {
        TypeMirror caughtException = successor.getKey();
        if (types.isSubtype(caughtException, javaLangExceptionType)) {
          AccumulationStore resultStore = successor.getValue();
          resultStore.insertOrRefine(e, calledMethod);
        }
      }
    }
  }

  /**
   * Extract the current called-methods type from {@code currentType}, and then add each element of
   * {@code methodNames} to it, and return the result. This method is similar to GLB, but should be
   * used when the new methods come from a source other than an {@code CalledMethods} annotation.
   *
   * @param currentType the current type in the called-methods hierarchy
   * @param methodNames the names of the new methods to add to the type
   * @return the new annotation to be added to the type, or null if the current type cannot be
   *     converted to an accumulator annotation
   */
  private @Nullable AnnotationMirror getUpdatedCalledMethodsType(
      AnnotatedTypeMirror currentType, List<String> methodNames) {
    AnnotationMirror type;
    if (currentType == null || !currentType.hasPrimaryAnnotationInHierarchy(atypeFactory.top)) {
      type = atypeFactory.top;
    } else {
      type = currentType.getPrimaryAnnotationInHierarchy(atypeFactory.top);
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
