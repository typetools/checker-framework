package org.checkerframework.common.accumulation;

import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.framework.flow.CFAbstractStore;
import org.checkerframework.framework.flow.CFAbstractTransfer;
import org.checkerframework.javacutil.AnnotationMirrorSet;
import org.plumelib.util.CollectionsPlume;

/**
 * The default transfer function for an accumulation checker.
 *
 * <p>Subclasses should call the {@link #accumulate(Node, TransferResult, String...)} accumulate}
 * method to add a string to the estimate at a particular program point.
 */
public class AccumulationTransfer
    extends CFAbstractTransfer<AccumulationValue, AccumulationStore, AccumulationTransfer> {

  /** The type factory. */
  protected final AccumulationAnnotatedTypeFactory atypeFactory;

  /**
   * Build a new AccumulationTransfer for the given analysis.
   *
   * @param analysis the analysis
   */
  public AccumulationTransfer(AccumulationAnalysis analysis) {
    super(analysis);
    atypeFactory = (AccumulationAnnotatedTypeFactory) analysis.getTypeFactory();
  }

  /**
   * Updates the estimate of how many things {@code node} has accumulated.
   *
   * <p>If the node is an invocation of a method that returns its receiver, then its receiver's type
   * will also be updated. In a chain of method calls, this process will continue backward as long
   * as each receiver is itself a receiver-returning method invocation.
   *
   * <p>For example, suppose {@code node} is the expression {@code a.b().c()}, the new value (added
   * by the accumulation analysis because of the {@code .c()} call) is "foo", and b and c return
   * their receiver. This method will directly update the estimate of {@code a.b().c()} to include
   * "foo". In addition, the estimates for the expressions {@code a.b()} and {@code a} would have
   * their estimates updated to include "foo", because c and b (respectively) return their
   * receivers. Note that due to what kind of values can be held in the store, this information is
   * lost outside the method chain. That is, the returns-receiver propagated information is lost
   * outside the expression in which the returns-receiver method invocations are nested.
   *
   * <p>As a concrete example, consider the Called Methods accumulation checker: if {@code build}
   * requires a, b, and c to be called, then {@code foo.a().b().c().build();} will typecheck (they
   * are in one fluent method chain), but {@code foo.a().b().c(); foo.build();} will not -- the
   * store does not keep the information that a, b, and c have been called outside the chain. {@code
   * foo}'s type will be {@code CalledMethods("a")}, because only {@code a()} was called directly on
   * {@code foo}. For such code to typecheck, the Called Methods accumulation checker uses an
   * additional rule: the return type of a receiver-returning method {@code rr()} is {@code
   * CalledMethods("rr")}. This rule is implemented directly in the {@link
   * org.checkerframework.framework.type.treeannotator.TreeAnnotator} subclass defined in the Called
   * Methods type factory.
   *
   * @param node the node whose estimate should be expanded
   * @param result the transfer result containing the store to be modified
   * @param values the new accumulation values
   */
  public void accumulate(
      Node node, TransferResult<AccumulationValue, AccumulationStore> result, String... values) {
    List<String> valuesAsList = Arrays.asList(values);
    JavaExpression target = JavaExpression.fromNode(node);
    if (CFAbstractStore.canInsertJavaExpression(target)) {
      if (result.containsTwoStores()) {
        updateValueAndInsertIntoStore(result.getThenStore(), target, valuesAsList);
        updateValueAndInsertIntoStore(result.getElseStore(), target, valuesAsList);
      } else {
        updateValueAndInsertIntoStore(result.getRegularStore(), target, valuesAsList);
      }
    }

    Tree tree = node.getTree();
    if (tree != null && tree.getKind() == Tree.Kind.METHOD_INVOCATION) {
      Node receiver = ((MethodInvocationNode) node).getTarget().getReceiver();
      if (receiver != null && atypeFactory.returnsThis((MethodInvocationTree) tree)) {
        accumulate(receiver, result, values);
      }
    }
  }

  /**
   * Updates {@code target} in {@code store} so that {@code store}'s estimate includes the
   * newly-accumulated values in {@code values}. If dataflow has already recorded information about
   * the target, this method fetches it and integrates it into the list of values in the new
   * annotation.
   *
   * @param store a store
   * @param target an insertable JavaExpression ({@code canInsertJavaExpression(target)} should have
   *     returned true)
   * @param values a list of newly-accumulated values
   */
  protected void updateValueAndInsertIntoStore(
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
}
