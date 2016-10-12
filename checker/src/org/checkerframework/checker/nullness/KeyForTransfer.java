package org.checkerframework.checker.nullness;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.KeyFor;
import org.checkerframework.checker.nullness.qual.UnknownKeyFor;
import org.checkerframework.dataflow.analysis.ConditionalTransferResult;
import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.dataflow.analysis.FlowExpressions.Receiver;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.framework.flow.CFAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFTransfer;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TypesUtils;

/*
 * KeyForTransfer ensures that java.util.Map.put and containsKey
 * cause the appropriate @KeyFor annotation to be added to the key.
 */
public class KeyForTransfer extends CFTransfer {

    protected final AnnotationMirror UNKNOWNKEYFOR, KEYFOR;

    public KeyForTransfer(CFAnalysis analysis) {
        super(analysis);
        UNKNOWNKEYFOR =
                AnnotationUtils.fromClass(
                        analysis.getTypeFactory().getElementUtils(), UnknownKeyFor.class);
        KEYFOR =
                AnnotationUtils.fromClass(
                        analysis.getTypeFactory().getElementUtils(), KeyFor.class);
    }

    /*
     * Provided that m is of a type that implements interface java.util.Map:
     * -Given a call m.containsKey(k), ensures that k is @KeyFor("m") in the thenStore of the transfer result.
     * -Given a call m.put(k, ...), ensures that k is @KeyFor("m") in the thenStore and elseStore of the transfer result.
     */
    @Override
    public TransferResult<CFValue, CFStore> visitMethodInvocation(
            MethodInvocationNode node, TransferInput<CFValue, CFStore> in) {

        TransferResult<CFValue, CFStore> result = super.visitMethodInvocation(node, in);

        String methodName = node.getTarget().getMethod().toString();

        // First verify if the method name is containsKey or put. This is an inexpensive check.

        boolean containsKey = methodName.startsWith("containsKey(");
        boolean put = methodName.startsWith("put(");

        if (containsKey || put) {
            // Now verify that the receiver of the method invocation is of a type
            // that extends that java.util.Map interface. This is a more expensive check.

            javax.lang.model.util.Types types = analysis.getTypes();

            TypeMirror mapInterfaceTypeMirror =
                    types.erasure(
                            TypesUtils.typeFromClass(
                                    types, analysis.getEnv().getElementUtils(), Map.class));

            TypeMirror receiverType = types.erasure(node.getTarget().getReceiver().getType());

            if (types.isSubtype(receiverType, mapInterfaceTypeMirror)) {
                KeyForAnnotatedTypeFactory atypeFactory =
                        (KeyForAnnotatedTypeFactory) analysis.getTypeFactory();
                Node receiver = node.getTarget().getReceiver();
                Receiver internalReceiver = FlowExpressions.internalReprOf(atypeFactory, receiver);
                String mapName = internalReceiver.toString();
                Receiver keyReceiver =
                        FlowExpressions.internalReprOf(atypeFactory, node.getArgument(0));

                LinkedHashSet<String> keyForMaps = new LinkedHashSet<>();
                keyForMaps.add(mapName);

                final CFValue previousKeyValue = in.getValueOfSubNode(node.getArgument(0));
                if (previousKeyValue != null) {
                    final AnnotationMirror prevAm =
                            atypeFactory
                                    .getQualifierHierarchy()
                                    .findAnnotationInHierarchy(
                                            previousKeyValue.getAnnotations(), UNKNOWNKEYFOR);
                    if (prevAm != null && AnnotationUtils.areSameByClass(prevAm, KeyFor.class)) {
                        keyForMaps.addAll(getKeys(prevAm));
                    }
                }

                AnnotationMirror am =
                        atypeFactory.createKeyForAnnotationMirrorWithValue(keyForMaps);

                if (containsKey) {
                    ConditionalTransferResult<CFValue, CFStore> conditionalResult =
                            (ConditionalTransferResult<CFValue, CFStore>) result;
                    conditionalResult.getThenStore().insertValue(keyReceiver, am);
                } else if (put) {
                    result.getThenStore().insertValue(keyReceiver, am);
                    result.getElseStore().insertValue(keyReceiver, am);
                }
            }
        }

        return result;
    }

    /**
     * @return the String value of a KeyFor, this will throw an exception
     */
    private Set<String> getKeys(final AnnotationMirror keyFor) {
        if (keyFor.getElementValues().size() == 0) {
            return new LinkedHashSet<>();
        }

        return new LinkedHashSet<>(
                AnnotationUtils.getElementValueArray(keyFor, "value", String.class, true));
    }
}
