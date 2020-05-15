package org.checkerframework.common.accumulation;

import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.common.value.ValueCheckerUtils;
import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.dataflow.analysis.FlowExpressions.Receiver;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.framework.flow.CFAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFTransfer;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.type.AnnotatedTypeMirror;

/**
 * The default transfer function for an accumulation checker.
 *
 * <p>Subclasses should call the {@link #accumulate(Node, TransferResult, String...)} accumulate} or
 * {@link #accumulate(MethodInvocationNode, TransferResult, String...)} methods to accumulate a
 * string at a particular program point.
 */
public class AccumulationTransfer extends CFTransfer {

    /** The type factory. */
    protected final AccumulationAnnotatedTypeFactory typeFactory;

    /**
     * Required constructor.
     *
     * @param analysis the analysis
     */
    public AccumulationTransfer(CFAnalysis analysis) {
        super(analysis);
        typeFactory = (AccumulationAnnotatedTypeFactory) analysis.getTypeFactory();
    }

    /**
     * Updates the estimate of how many things {@code node} has accumulated.
     *
     * @param node the node whose estimate should be expanded
     * @param result the transfer result containing the store to be modified
     * @param values the new accumulation values
     */
    public void accumulate(Node node, TransferResult<CFValue, CFStore> result, String... values) {
        AnnotatedTypeMirror oldType = typeFactory.getAnnotatedType(node.getTree());
        AnnotationMirror newAnno = getNewAnno(oldType, values);
        insertIntoStores(result, node, newAnno);
    }

    /**
     * Updates the estimate for the receiver and any other receiver-returning methods in a chain
     * with this MethodInvocationNode to include the new values.
     *
     * <p>For example, if the argument is the expression {@code a.b().c()}, the new value is "foo",
     * and b and c return their receiver (and are deterministic), all of the expressions {@code a},
     * {@code a.b()}, and {@code a.b().c()} would have their estimates updated to include "foo". If
     * any method in the chain is non-deterministic, its estimate will not be updated (but the rest
     * of the chain is not affected).
     *
     * @param node a method invocation whose receiver is to be updated
     * @param result the result containing the store to be modified
     * @param values the new accumulation values
     */
    public void accumulate(
            MethodInvocationNode node, TransferResult<CFValue, CFStore> result, String... values) {
        Node receiver = node.getTarget().getReceiver();
        AnnotatedTypeMirror oldType = typeFactory.getReceiverType(node.getTree());
        // e.g. if the node being visited is static
        if (oldType == null) {
            return;
        }
        AnnotationMirror newAnno = getNewAnno(oldType, values);
        while (receiver != null) {
            // Note that this call doesn't do anything if receiver is a method call
            // that is not deterministic, though it can still continue to recurse.
            insertIntoStores(result, receiver, newAnno);

            Tree receiverTree = receiver.getTree();
            // Possibly recurse: if the receiver is itself a method call,
            // then we need to also propagate this new information to its receiver
            // if the method being called has an @This return type.
            //
            // Note that we must check for null, because the tree could be
            // implicit (when calling an instance method on the class itself).
            // In that case, do not attempt to refine either - the receiver is
            // not a method invocation, anyway.
            if (receiverTree == null || receiverTree.getKind() != Tree.Kind.METHOD_INVOCATION) {
                // Do not continue, because the receiver isn't a method invocation itself. The
                // end of the chain of calls has been reached.
                break;
            }

            MethodInvocationTree receiverAsMethodInvocation =
                    (MethodInvocationTree) receiver.getTree();

            if (typeFactory.returnsThis(receiverAsMethodInvocation)) {
                receiver = ((MethodInvocationNode) receiver).getTarget().getReceiver();
            } else {
                // Do not continue, because the method does not return @This.
                break;
            }
        }
    }

    /**
     * Combines the values in oldType with the values in newValues to produce a single accumulator
     * type qualifier.
     *
     * @param oldType an annotated type mirror whose values should be included
     * @param newValues new values to include
     * @return an annotation representing all the values
     */
    private AnnotationMirror getNewAnno(AnnotatedTypeMirror oldType, String[] newValues) {
        AnnotationMirror oldAnno;
        if (oldType == null) {
            oldAnno = typeFactory.top;
        } else {
            oldAnno = oldType.getAnnotationInHierarchy(typeFactory.top);
            if (oldAnno == null || !typeFactory.isAccumulatorAnnotation(oldAnno)) {
                oldAnno = typeFactory.top;
            }
        }
        String[] allValues;
        if (typeFactory.isAccumulatorAnnotation(oldAnno)) {
            List<String> oldTypeValues =
                    ValueCheckerUtils.getValueOfAnnotationWithStringArgument(oldAnno);
            for (String newValue : newValues) {
                oldTypeValues.add(newValue);
            }
            allValues = oldTypeValues.toArray(new String[0]);
        } else {
            allValues = newValues;
        }
        return typeFactory.createAccumulatorAnnotation(allValues);
    }

    /**
     * Inserts newAnno as the value into all stores (conditional or not) in result for node.
     *
     * @param result the TransferResult holding the stores to modify
     * @param node the node whose value should be modified
     * @param newAnno the new value
     */
    private void insertIntoStores(
            TransferResult<CFValue, CFStore> result, Node node, AnnotationMirror newAnno) {
        Receiver receiver = FlowExpressions.internalReprOf(typeFactory, node);
        if (result.containsTwoStores()) {
            CFStore thenStore = result.getThenStore();
            CFStore elseStore = result.getElseStore();
            thenStore.insertValue(receiver, newAnno);
            elseStore.insertValue(receiver, newAnno);
        } else {
            CFStore store = result.getRegularStore();
            store.insertValue(receiver, newAnno);
        }
    }
}
