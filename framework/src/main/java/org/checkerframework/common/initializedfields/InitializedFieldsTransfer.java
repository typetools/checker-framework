package org.checkerframework.common.initializedfields;

import com.sun.source.util.TreePath;
import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import org.checkerframework.common.accumulation.AccumulationTransfer;
import org.checkerframework.common.initializedfields.qual.InitializesFields;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.AssignmentNode;
import org.checkerframework.dataflow.cfg.node.FieldAccessNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.expression.Receiver;
import org.checkerframework.framework.flow.CFAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.util.FlowExpressionParseUtil;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;

/** Accumulates the names of fields that are initialized. */
public class InitializedFieldsTransfer extends AccumulationTransfer {

    /**
     * default constructor
     *
     * @param analysis the analysis
     */
    public InitializedFieldsTransfer(final CFAnalysis analysis) {
        super(analysis);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitAssignment(
            final AssignmentNode node, final TransferInput<CFValue, CFStore> input) {
        TransferResult<CFValue, CFStore> result = super.visitAssignment(node, input);
        Node lhs = node.getTarget();
        if (lhs instanceof FieldAccessNode) {
            FieldAccessNode fieldAccess = (FieldAccessNode) lhs;
            Node receiver = fieldAccess.getReceiver();
            String fieldName = fieldAccess.getFieldName();
            accumulate(receiver, result, fieldName);
        }
        return result;
    }

    // Handles @InitializesFields method postcondition annotation
    @Override
    public TransferResult<CFValue, CFStore> visitMethodInvocation(
            final MethodInvocationNode node, final TransferInput<CFValue, CFStore> input) {
        TransferResult<CFValue, CFStore> result = super.visitMethodInvocation(node, input);

        ExecutableElement method = node.getTarget().getMethod();
        AnnotationMirror anno = atypeFactory.getDeclAnnotation(method, InitializesFields.class);
        if (anno != null) {
            String objectWithFieldsExpr =
                    AnnotationUtils.getElementValue(anno, "fields", String.class, true);

            Receiver objectWithFields;
            {
                TreePath currentPath = atypeFactory.getPath(node.getTree());
                if (currentPath == null) {
                    throw new BugInCF("Why no currentPath?");
                }
                try {
                    objectWithFields =
                            atypeFactory.getReceiverFromJavaExpressionString(
                                    objectWithFieldsExpr, currentPath);
                } catch (FlowExpressionParseUtil.FlowExpressionParseException e) {
                    // TODO: report error
                    return result;
                }
            }

            List<String> newFields =
                    AnnotationUtils.getElementValueArray(anno, "fields", String.class, false);
            accumulate(objectWithFields, result, newFields.toArray(new String[newFields.size()]));
        }
        return result;
    }
}
