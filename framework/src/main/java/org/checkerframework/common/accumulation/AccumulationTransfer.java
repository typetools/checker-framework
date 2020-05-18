package org.checkerframework.common.accumulation;

import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.checker.nullness.qual.Nullable;
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
 * <p>Subclasses should call the {@link #accumulate(Node, TransferResult, String...)} accumulate}
 * method to accumulate a string at a particular program point.
 */
public class AccumulationTransfer extends CFTransfer {

    /** The type factory. */
    protected final AccumulationAnnotatedTypeFactory typeFactory;

    /**
     * Build a new AccumulationTransfer for the given analysis.
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
     * <p>If the node is an invocation of a method that returns its receiver, then its receiver's
     * type will also be updated. This chain will continue as long as each receiver is itself a
     * receiver-returning method invocation.
     *
     * <p>For example, if the argument is the expression {@code a.b().c()}, the new value is "foo",
     * and b and c return their receiver (and are deterministic), all of the expressions {@code a},
     * {@code a.b()}, and {@code a.b().c()} would have their estimates updated to include "foo".
     *
     * <p>If any method in the chain is non-deterministic, its estimate will not be updated (but the
     * types of any deterministic methods, and the ultimate receiver, will be updated).
     *
     * @param node the node whose estimate should be expanded
     * @param result the transfer result containing the store to be modified
     * @param values the new accumulation values
     */
    public void accumulate(Node node, TransferResult<CFValue, CFStore> result, String... values) {
        Tree tree = node.getTree();
        if (tree == null) {
            return;
        }
        AnnotatedTypeMirror oldType = typeFactory.getAnnotatedType(tree);
        AnnotationMirror newAnno = getCombinedAnno(oldType, Arrays.asList(values));
        insertIntoStores(result, node, newAnno);

        if (tree.getKind() == Kind.METHOD_INVOCATION) {
            MethodInvocationNode methodInvocationNode = (MethodInvocationNode) node;
            Node receiver = methodInvocationNode.getTarget().getReceiver();
            MethodInvocationTree invokedMethod = (MethodInvocationTree) tree;

            while (receiver != null && typeFactory.returnsThis(invokedMethod)) {
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

                invokedMethod = (MethodInvocationTree) receiver.getTree();
                receiver = ((MethodInvocationNode) receiver).getTarget().getReceiver();
            }
        }
    }

    /**
     * Unions the values in oldType with the values in newValues to produce a single accumulator
     * type qualifier.
     *
     * @param oldType an annotated type mirror whose values should be included
     * @param newValues new values to include.
     * @return an annotation representing all the values
     */
    private AnnotationMirror getCombinedAnno(
            @Nullable AnnotatedTypeMirror oldType, List<String> newValues) {
        AnnotationMirror oldAnno;
        if (oldType == null) {
            oldAnno = typeFactory.top;
        } else {
            oldAnno = oldType.getAnnotationInHierarchy(typeFactory.top);
            if (oldAnno == null) {
                oldAnno = typeFactory.top;
            }
        }
        List<String> allValues = new ArrayList<>(newValues);
        if (typeFactory.isAccumulatorAnnotation(oldAnno)) {
            List<String> oldTypeValues =
                    ValueCheckerUtils.getValueOfAnnotationWithStringArgument(oldAnno);
            allValues.addAll(oldTypeValues);
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
