package org.checkerframework.checker.determinism;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFTransfer;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TypesUtils;

public class DeterminismTransfer extends CFTransfer {
    public DeterminismTransfer(CFAbstractAnalysis<CFValue, CFStore, CFTransfer> analysis) {
        super(analysis);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitMethodInvocation(
            MethodInvocationNode n, TransferInput<CFValue, CFStore> in) {
        TransferResult<CFValue, CFStore> result = super.visitMethodInvocation(n, in);
        DeterminismAnnotatedTypeFactory factory =
                (DeterminismAnnotatedTypeFactory) analysis.getTypeFactory();

        // Type refinement for List sort
        Node receiver = n.getTarget().getReceiver();
        if (receiver == null || TypesUtils.getTypeElement(receiver.getType()) == null) {
            return result;
        }
        TypeMirror underlyingType = TypesUtils.getTypeElement(receiver.getType()).asType();

        boolean isList = factory.isList(underlyingType);
        if (isList) {
            String methName = getMethodName(n.toString(), receiver);
            if (methName.equals("sort") && receiver.getType().getAnnotationMirrors().size() > 0) {
                // Check if receiver has OrderNonDet annotation
                AnnotationMirror receiverAnno =
                        receiver.getType().getAnnotationMirrors().iterator().next();
                if (receiverAnno != null
                        && AnnotationUtils.areSame(receiverAnno, factory.ORDERNONDET)) {
                    FlowExpressions.Receiver sortReceiver =
                            FlowExpressions.internalReprOf(factory, n.getTarget().getReceiver());
                    result.getThenStore().insertValue(sortReceiver, factory.DET);
                    result.getElseStore().insertValue(sortReceiver, factory.DET);
                }
            }
        }

        // Type refinement for Arrays sort
        boolean isArrays = factory.isArrays(underlyingType);
        if (isArrays) {
            String methName = getMethodName(n.toString(), receiver);
            if ((methName.equals("sort") || methName.equals("parallelSort"))
            /*&& n.getArguments().size() == 1*/ ) {
                AnnotatedTypeMirror firstArg =
                        factory.getAnnotatedType(n.getTree().getArguments().get(0));
                AnnotationMirror firstArgAnno = firstArg.getAnnotations().iterator().next();
                // Check if receiver has first argument annotation
                if (firstArgAnno != null
                        && AnnotationUtils.areSame(firstArgAnno, factory.ORDERNONDET)) {
                    FlowExpressions.Receiver firtArgRep =
                            FlowExpressions.internalReprOf(factory, n.getArgument(0));
                    result.getThenStore().insertValue(firtArgRep, factory.DET);
                    result.getElseStore().insertValue(firtArgRep, factory.DET);
                }
            }
        }

        // Type refinement for Collections
        boolean isCollections = factory.isCollections(underlyingType);
        if (isCollections) {
            String methName = getMethodName(n.toString(), receiver);
            // refinement for sort
            if (methName.equals("sort")) {
                AnnotatedTypeMirror firstArg =
                        factory.getAnnotatedType(n.getTree().getArguments().get(0));
                AnnotationMirror firstArgAnno = firstArg.getAnnotations().iterator().next();
                // Check if receiver has first argument annotation
                if (firstArgAnno != null
                        && AnnotationUtils.areSame(firstArgAnno, factory.ORDERNONDET)) {
                    FlowExpressions.Receiver firtArgRep =
                            FlowExpressions.internalReprOf(factory, n.getArgument(0));
                    result.getThenStore().insertValue(firtArgRep, factory.DET);
                    result.getElseStore().insertValue(firtArgRep, factory.DET);
                }
            }

            // refinement for shuffle
            if (methName.equals("shuffle")) {
                AnnotatedTypeMirror firstArg =
                        factory.getAnnotatedType(n.getTree().getArguments().get(0));
                AnnotationMirror firstArgAnno = firstArg.getAnnotations().iterator().next();
                // Check if receiver has first argument annotation
                FlowExpressions.Receiver firtArgRep =
                        FlowExpressions.internalReprOf(factory, n.getArgument(0));
                result.getThenStore().insertValue(firtArgRep, factory.NONDET);
                result.getElseStore().insertValue(firtArgRep, factory.NONDET);
            }
        }
        return result;
    }

    String getMethodName(String n, Node receiver) {
        String methodName = n.toString();
        String methodnameWithoutReceiver = methodName.substring(receiver.toString().length());
        int startIndex = methodnameWithoutReceiver.indexOf(".");
        int endIndex = methodnameWithoutReceiver.indexOf("(");
        String methName = methodnameWithoutReceiver.substring(startIndex + 1, endIndex);
        return methName;
    }
}
