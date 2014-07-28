package org.checkerframework.checker.nullness;

import java.util.ArrayList;
import java.util.Map;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeMirror;

import org.checkerframework.checker.nullness.qual.KeyFor;
import org.checkerframework.dataflow.analysis.ConditionalTransferResult;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.analysis.FlowExpressions.Receiver;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.framework.flow.CFAbstractTransfer;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.util.AnnotationBuilder;
import org.checkerframework.framework.util.FlowExpressionParseUtil;
import org.checkerframework.framework.util.FlowExpressionParseUtil.FlowExpressionContext;
import org.checkerframework.javacutil.TypesUtils;

/*
 * KeyForTransfer ensures that java.util.Map.put and containsKey
 * cause the appropriate @KeyFor annotation to be added to the key.
 */
public class KeyForTransfer extends
    CFAbstractTransfer<CFValue, CFStore, KeyForTransfer> {

    /** Type-specific version of super.analysis and super.checker. */
    protected KeyForAnalysis analysis;
    protected KeyForSubchecker checker;

    public KeyForTransfer(KeyForAnalysis analysis, KeyForSubchecker checker) {
        super(analysis);
        this.analysis = analysis;
        this.checker = checker;
    }

    /*
     * Given a string 'value', returns an AnnotationMirror corresponding to @KeyFor(value)
     */
    private AnnotationMirror getKeyForAnnotationMirrorWithValue(String value) {
        // Create an ArrayList with the value

        ArrayList<String> values = new ArrayList<String>();

        values.add(value);

        // Create an AnnotationBuilder with the ArrayList

        AnnotationBuilder builder =
                new AnnotationBuilder(analysis.getTypeFactory().getProcessingEnv(), KeyFor.class);
        builder.setValue("value", values);

        // Return the resulting AnnotationMirror

        return builder.build();
    }

    /*
     * Provided that m is of a type that implements interface java.util.Map:
     * -Given a call m.containsKey(k), ensures that k is @KeyFor("m") in the thenStore of the transfer result.
     * -Given a call m.put(k, ...), ensures that k is @KeyFor("m") in the thenStore and elseStore of the transfer result.
     */
    @Override
    public TransferResult<CFValue, CFStore> visitMethodInvocation(MethodInvocationNode node,
            TransferInput<CFValue, CFStore> in) {

        TransferResult<CFValue, CFStore> result = super.visitMethodInvocation(node, in);

        String methodName = node.getTarget().getMethod().toString();

        // First verify if the method name is containsKey or put. This is an inexpensive check.

        boolean containsKey = methodName.startsWith("containsKey(");
        boolean put = methodName.startsWith("put(");

        if (containsKey || put) {
            // Now verify that the receiver of the method invocation is of a type
            // that extends that java.util.Map interface. This is a more expensive check.

            javax.lang.model.util.Types types = analysis.getTypes();

            TypeMirror mapInterfaceTypeMirror = types.erasure(TypesUtils.typeFromClass(types, analysis.getEnv().getElementUtils(), Map.class));

            TypeMirror receiverType = types.erasure(node.getTarget().getReceiver().getType());

            if (types.isSubtype(receiverType, mapInterfaceTypeMirror)) {

                FlowExpressionContext flowExprContext = FlowExpressionParseUtil
                        .buildFlowExprContextForUse(node, analysis.getTypeFactory());

                String mapName = flowExprContext.receiver.toString();
                Receiver keyReceiver = flowExprContext.arguments.get(0);
                AnnotationMirror am = getKeyForAnnotationMirrorWithValue(mapName); // @KeyFor(mapName)

                if (containsKey) {
                    ConditionalTransferResult<CFValue, CFStore> conditionalResult = (ConditionalTransferResult<CFValue, CFStore>) result;
                    conditionalResult.getThenStore().insertValue(keyReceiver, am);
                } else if (put) {
                    result.getThenStore().insertValue(keyReceiver, am);
                    result.getElseStore().insertValue(keyReceiver, am);
                }
            }
        }

        return result;
    }
}