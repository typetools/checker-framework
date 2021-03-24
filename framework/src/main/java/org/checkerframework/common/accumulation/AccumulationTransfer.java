package org.checkerframework.common.accumulation;

import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.common.value.ValueCheckerUtils;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.framework.flow.CFAbstractStore;
import org.checkerframework.framework.flow.CFAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFTransfer;
import org.checkerframework.framework.flow.CFValue;

/**
 * The default transfer function for an accumulation checker.
 *
 * <p>Subclasses should call the {@link #accumulate(Node, TransferResult, String...)} accumulate}
 * method to add a string to the estimate at a particular program point.
 */
public class AccumulationTransfer extends CFTransfer {

    /** The type factory. */
    protected final AccumulationAnnotatedTypeFactory atypeFactory;

    /**
     * Build a new AccumulationTransfer for the given analysis.
     *
     * @param analysis the analysis
     */
    public AccumulationTransfer(CFAnalysis analysis) {
        super(analysis);
        atypeFactory = (AccumulationAnnotatedTypeFactory) analysis.getTypeFactory();
    }

    /**
     * Updates the estimate of how many things {@code node} has accumulated.
     *
     * <p>If the node is an invocation of a method that returns its receiver, then its receiver's
     * type will also be updated. In a chain of method calls, this process will continue backward as
     * long as each receiver is itself a receiver-returning method invocation.
     *
     * <p>For example, suppose {@code node} is the expression {@code a.b().c()}, the new value
     * (added by the accumulation analysis because of the {@code .c()} call) is "foo", and b and c
     * return their receiver. This method will directly update the estimate of {@code a.b().c()} to
     * include "foo". In addition, the estimates for the expressions {@code a.b()} and {@code a}
     * would have their estimates updated to include "foo", because c and b (respectively) return
     * their receivers. Note that due to what kind of values can be held in the store, this
     * information is lost outside the method chain. That is, the returns-receiver propagated
     * information is lost outside the expression in which the returns-receiver method invocations
     * are nested.
     *
     * <p>As a concrete example, consider the Called Methods accumulation checker: if {@code build}
     * requires a, b, and c to be called, then {@code foo.a().b().c().build();} will typecheck (they
     * are in one fluent method chain), but {@code foo.a().b().c(); foo.build();} will not -- the
     * store does not keep the information that a, b, and c have been called outside the chain.
     * {@code foo}'s type will be {@code CalledMethods("a")}, because only {@code a()} was called
     * directly on {@code foo}. For such code to typecheck, the Called Methods accumulation checker
     * uses an additional rule: the return type of a receiver-returning method {@code rr()} is
     * {@code CalledMethods("rr")}. This rule is implemented directly in the {@link
     * org.checkerframework.framework.type.treeannotator.TreeAnnotator} subclass defined in the
     * Called Methods type factory.
     *
     * @param node the node whose estimate should be expanded
     * @param result the transfer result containing the store to be modified
     * @param values the new accumulation values
     */
    public void accumulate(Node node, TransferResult<CFValue, CFStore> result, String... values) {
        List<String> valuesAsList = Arrays.asList(values);
        // If dataflow has already recorded information about the target, fetch it and integrate
        // it into the list of values in the new annotation.
        JavaExpression target = JavaExpression.fromNode(node);
        if (CFAbstractStore.canInsertJavaExpression(target)) {
            CFValue flowValue = result.getRegularStore().getValue(target);
            if (flowValue != null) {
                Set<AnnotationMirror> flowAnnos = flowValue.getAnnotations();
                assert flowAnnos.size() <= 1;
                for (AnnotationMirror anno : flowAnnos) {
                    if (atypeFactory.isAccumulatorAnnotation(anno)) {
                        List<String> oldFlowValues =
                                ValueCheckerUtils.getValueOfAnnotationWithStringArgument(anno);
                        if (oldFlowValues != null) {
                            // valuesAsList cannot have its length changed -- it is backed by an
                            // array -- but oldFlowValues is a new, modifiable list.
                            oldFlowValues.addAll(valuesAsList);
                            valuesAsList = oldFlowValues;
                        }
                    }
                }
            }
        }

        AnnotationMirror newAnno = atypeFactory.createAccumulatorAnnotation(valuesAsList);
        insertIntoStores(result, target, newAnno);

        Tree tree = node.getTree();
        if (tree != null && tree.getKind() == Kind.METHOD_INVOCATION) {
            Node receiver = ((MethodInvocationNode) node).getTarget().getReceiver();
            if (receiver != null && atypeFactory.returnsThis((MethodInvocationTree) tree)) {
                accumulate(receiver, result, values);
            }
        }
    }

    /**
     * Inserts newAnno as the value into all stores (conditional or not) in the result for node.
     *
     * @param result the TransferResult holding the stores to modify
     * @param target the receiver whose value should be modified
     * @param newAnno the new value
     */
    private void insertIntoStores(
            TransferResult<CFValue, CFStore> result,
            JavaExpression target,
            AnnotationMirror newAnno) {
        if (result.containsTwoStores()) {
            CFStore thenStore = result.getThenStore();
            CFStore elseStore = result.getElseStore();
            thenStore.insertValue(target, newAnno);
            elseStore.insertValue(target, newAnno);
        } else {
            CFStore store = result.getRegularStore();
            store.insertValue(target, newAnno);
        }
    }
}
