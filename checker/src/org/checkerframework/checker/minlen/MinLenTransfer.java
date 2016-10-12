package org.checkerframework.checker.minlen;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;

import org.checkerframework.checker.minlen.qual.MinLen;
import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.dataflow.analysis.FlowExpressions.Receiver;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.framework.flow.CFAbstractTransfer;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.TreeUtils;

public class MinLenTransfer extends CFAbstractTransfer<MinLenValue, MinLenStore, MinLenTransfer> {

    protected MinLenAnalysis analysis;
    protected static MinLenAnnotatedTypeFactory atypeFactory;
    protected final ProcessingEnvironment env;

    public MinLenTransfer(MinLenAnalysis analysis) {
        super(analysis);
        this.analysis = analysis;
        atypeFactory = (MinLenAnnotatedTypeFactory) analysis.getTypeFactory();
        this.env = MinLenAnnotatedTypeFactory.env;
    }

    @Override
    public TransferResult<MinLenValue, MinLenStore> visitMethodInvocation(MethodInvocationNode node, TransferInput<MinLenValue, MinLenStore> in) {
        TransferResult<MinLenValue, MinLenStore> result = super.visitMethodInvocation(node, in);
        Receiver rec = FlowExpressions.internalReprOf(analysis.getTypeFactory(), node.getTarget().getReceiver());
        if (TreeUtils.isMethodInvocation(node.getTree(), TreeUtils.getMethod("java.util.List", "add", 0, env), env)) {
            AnnotatedTypeMirror ATM = atypeFactory.getAnnotatedType(node.getTarget().getReceiver().getTree());
            AnnotationMirror anno = ATM.getAnnotation(MinLen.class);
            int value = MinLenAnnotatedTypeFactory.getMinLenValue(anno);
            result.getRegularStore().insertValue(rec, atypeFactory.createMinLen(value + 1));
        }
        return result;
    }
}
