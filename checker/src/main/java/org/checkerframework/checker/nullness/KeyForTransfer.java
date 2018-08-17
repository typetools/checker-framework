package org.checkerframework.checker.nullness;

import java.util.LinkedHashSet;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.checker.nullness.qual.KeyFor;
import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.dataflow.analysis.FlowExpressions.Receiver;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.framework.flow.CFAbstractTransfer;
import org.checkerframework.javacutil.AnnotationUtils;

/*
 * KeyForTransfer ensures that java.util.Map.put and containsKey
 * cause the appropriate @KeyFor annotation to be added to the key.
 */
public class KeyForTransfer extends CFAbstractTransfer<KeyForValue, KeyForStore, KeyForTransfer> {

    public KeyForTransfer(KeyForAnalysis analysis) {
        super(analysis);
    }

    /*
     * Provided that m is of a type that implements interface java.util.Map:
     * <ul>
     * <li>Given a call m.containsKey(k), ensures that k is @KeyFor("m") in the thenStore of the transfer result.
     * <li>Given a call m.put(k, ...), ensures that k is @KeyFor("m") in the thenStore and elseStore of the transfer result.
     * </ul>
     */
    @Override
    public TransferResult<KeyForValue, KeyForStore> visitMethodInvocation(
            MethodInvocationNode node, TransferInput<KeyForValue, KeyForStore> in) {

        TransferResult<KeyForValue, KeyForStore> result = super.visitMethodInvocation(node, in);
        KeyForAnnotatedTypeFactory factory = (KeyForAnnotatedTypeFactory) analysis.getTypeFactory();
        if (factory.isInvocationOfMapMethod(node, "containsKey")
                || factory.isInvocationOfMapMethod(node, "put")) {

            Node receiver = node.getTarget().getReceiver();
            Receiver internalReceiver = FlowExpressions.internalReprOf(factory, receiver);
            String mapName = internalReceiver.toString();
            Receiver keyReceiver = FlowExpressions.internalReprOf(factory, node.getArgument(0));

            LinkedHashSet<String> keyForMaps = new LinkedHashSet<>();
            keyForMaps.add(mapName);

            final KeyForValue previousKeyValue = in.getValueOfSubNode(node.getArgument(0));
            if (previousKeyValue != null) {
                for (AnnotationMirror prevAm : previousKeyValue.getAnnotations()) {
                    if (prevAm != null && AnnotationUtils.areSameByClass(prevAm, KeyFor.class)) {
                        keyForMaps.addAll(getKeys(prevAm));
                    }
                }
            }

            AnnotationMirror am = factory.createKeyForAnnotationMirrorWithValue(keyForMaps);

            if (factory.getMethodName(node).equals("containsKey")) {
                result.getThenStore().insertValue(keyReceiver, am);
            } else { // method name is "put"
                result.getThenStore().insertValue(keyReceiver, am);
                result.getElseStore().insertValue(keyReceiver, am);
            }
        }

        return result;
    }

    /** @return the String value of a KeyFor, this will throw an exception */
    private Set<String> getKeys(final AnnotationMirror keyFor) {
        if (keyFor.getElementValues().isEmpty()) {
            return new LinkedHashSet<>();
        }

        return new LinkedHashSet<>(
                AnnotationUtils.getElementValueArray(keyFor, "value", String.class, true));
    }
}
