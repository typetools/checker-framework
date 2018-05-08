//package org.checkerframework.checker.determinism;
//
//import org.checkerframework.dataflow.analysis.FlowExpressions;
//import org.checkerframework.dataflow.analysis.TransferInput;
//import org.checkerframework.dataflow.analysis.TransferResult;
//import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
//import org.checkerframework.dataflow.cfg.node.Node;
//import org.checkerframework.framework.flow.CFAbstractTransfer;
//import org.checkerframework.javacutil.AnnotationUtils;
//
//import javax.lang.model.element.AnnotationMirror;
//import java.util.LinkedHashSet;
//import java.util.Set;
//
///*
// * DeterminismTransfer ensures that java.util.List.sort
// * causes the receiver to get @Det annotation if it was @OrderNonDet.
// */
//public class DeterminismTransfer extends CFAbstractTransfer<DeterminismValue, DeterminismStore, DeterminismTransfer> {
//
//    public DeterminismTransfer(DeterminismAnalysis analysis) {
//        super(analysis);
//    }
//
//    /*
//     * Provided that m is of a type that implements interface java.util.Map:
//     * <ul>
//     * <li>Given a call m.containsKey(k), ensures that k is @KeyFor("m") in the thenStore of the transfer result.
//     * <li>Given a call m.put(k, ...), ensures that k is @KeyFor("m") in the thenStore and elseStore of the transfer result.
//     * </ul>
//     */
//    @Override
//    public TransferResult<DeterminismValue, DeterminismStore> visitMethodInvocation(
//            MethodInvocationNode node, TransferInput<DeterminismValue, DeterminismStore> in) {
//
//        TransferResult<DeterminismValue, DeterminismStore> result = super.visitMethodInvocation(node, in);
//        DeterminismAnnotatedTypeFactory factory = (DeterminismAnnotatedTypeFactory) analysis.getTypeFactory();
//        if (factory.isInvocationOfMapMethod(node, "containsKey")
//                || factory.isInvocationOfMapMethod(node, "put")) {
//
//            Node receiver = node.getTarget().getReceiver();
//            FlowExpressions.Receiver internalReceiver = FlowExpressions.internalReprOf(factory, receiver);
//            String mapName = internalReceiver.toString();
//            FlowExpressions.Receiver keyReceiver = FlowExpressions.internalReprOf(factory, node.getArgument(0));
//
//            LinkedHashSet<String> keyForMaps = new LinkedHashSet<>();
//            keyForMaps.add(mapName);
//
//            final KeyForValue previousKeyValue = in.getValueOfSubNode(node.getArgument(0));
//            if (previousKeyValue != null) {
//                for (AnnotationMirror prevAm : previousKeyValue.getAnnotations()) {
//                    if (prevAm != null && AnnotationUtils.areSameByClass(prevAm, KeyFor.class)) {
//                        keyForMaps.addAll(getKeys(prevAm));
//                    }
//                }
//            }
//
//            AnnotationMirror am = factory.createKeyForAnnotationMirrorWithValue(keyForMaps);
//
//            if (factory.getMethodName(node).equals("containsKey")) {
//                result.getThenStore().insertValue(keyReceiver, am);
//            } else { // method name is "put"
//                result.getThenStore().insertValue(keyReceiver, am);
//                result.getElseStore().insertValue(keyReceiver, am);
//            }
//        }
//
//        return result;
//    }
//
//}
//
