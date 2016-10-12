package org.checkerframework.checker.minlen;

import java.util.List;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
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
    protected final ExecutableElement listAdd;

    public MinLenTransfer(MinLenAnalysis analysis) {
        super(analysis);
        this.analysis = analysis;
        atypeFactory = (MinLenAnnotatedTypeFactory) analysis.getTypeFactory();
        this.env = MinLenAnnotatedTypeFactory.env;
        this.listAdd = TreeUtils.getMethod("java.util.List", "add", 1, env);
    }

    /*
        @Override
        public TransferResult<MinLenValue, MinLenStore> visitMethodInvocation(
                MethodInvocationNode node, TransferInput<MinLenValue, MinLenStore> in) {
            TransferResult<MinLenValue, MinLenStore> result = super.visitMethodInvocation(node, in);
            Receiver rec =
                    FlowExpressions.internalReprOf(
                            analysis.getTypeFactory(), node.getTarget().getReceiver());
            if (TreeUtils.isMethodInvocation(node.getTree(), listAdd, env)) {
                System.out.println("listAdd");
    		System.out.println(result.getRegularStore());
    		System.out.println("rec:" + rec);
                AnnotatedTypeMirror ATM =
                        atypeFactory.getAnnotatedType(node.getTarget().getReceiver().getTree());
                AnnotationMirror anno = ATM.getAnnotation(MinLen.class);
    		System.out.println(anno);
                int value = MinLenAnnotatedTypeFactory.getMinLenValue(anno);
    	    System.out.println(value);
                result.getRegularStore().insertValue(rec, atypeFactory.createMinLen(value + 1));
            }
    	System.out.println("Result:");
    	System.out.println(result.getRegularStore());
            return result;
        }
    */
}
