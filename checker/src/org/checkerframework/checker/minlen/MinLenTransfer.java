package org.checkerframework.checker.minlen;

import java.util.List;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import org.checkerframework.checker.minlen.qual.MinLen;
import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.dataflow.analysis.FlowExpressions.Receiver;
import org.checkerframework.dataflow.analysis.RegularTransferResult;
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
    protected final ExecutableElement listAdd;

    public MinLenTransfer(MinLenAnalysis analysis) {
        super(analysis);
        this.analysis = analysis;
        atypeFactory = (MinLenAnnotatedTypeFactory) analysis.getTypeFactory();
        this.env = MinLenAnnotatedTypeFactory.env;
        this.listAdd = TreeUtils.getMethod("java.util.List", "add", 1, env);
    }

    @Override
    public TransferResult<MinLenValue, MinLenStore> visitMethodInvocation(
            MethodInvocationNode node, TransferInput<MinLenValue, MinLenStore> in) {
        TransferResult<MinLenValue, MinLenStore> result = super.visitMethodInvocation(node, in);
        Receiver rec =
                FlowExpressions.internalReprOf(
                        analysis.getTypeFactory(), node.getTarget().getReceiver());
        MinLenStore store = result.getRegularStore();
        if (TreeUtils.isMethodInvocation(node.getTree(), listAdd, env)) {
            AnnotatedTypeMirror ATM =
                    atypeFactory.getAnnotatedType(node.getTarget().getReceiver().getTree());
            AnnotationMirror anno = ATM.getAnnotation(MinLen.class);
            int value = MinLenAnnotatedTypeFactory.getMinLenValue(anno);
            AnnotationMirror AM = atypeFactory.createMinLen(value + 1);
            store.clearValue(rec);
            store.insertValue(rec, AM);
            RegularTransferResult<MinLenValue, MinLenStore> newResult =
                    new RegularTransferResult<MinLenValue, MinLenStore>(
                            result.getResultValue(), store);
            return newResult;
        }
        return result;
    }
}
