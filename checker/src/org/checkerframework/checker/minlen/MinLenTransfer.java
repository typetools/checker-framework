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
       private void refineGT(
    		  Node left,
    		  AnnotatedTypeMirror leftType,
    		  Node right,
    		  AnnotatedTypeMirror rightType,
    		  MinLenStore store) {
    FieldAccessNode fi = null;
    // We only care about length. This will miss an expression which
    // include an array length (like "a.length + 1"), but that's okay
    // for now.
    // FIXME: Joe: List support will be needed here too.
    if (left instanceof FieldAccessNode) {
        fi = (FieldAccessNode)left;
    } else if (right instanceof FieldAccessNode) {
        fi = (FieldAccessNode)right;
    } else {
        return null;
    }
    if (fi.getFieldName().equals("length")
                   && fi.getReceiver().getType().getKind() == TypeKind.ARRAY) {
        // At this point, MinLen needs to invoke the constant value checker
        // to find out if it knows anything about what the length is being
        // compared to. If so, we can do something.
        return;
    }
    }*/

}
