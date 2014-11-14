package org.checkerframework.checker.regex;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;

import org.checkerframework.dataflow.analysis.ConditionalTransferResult;
import org.checkerframework.dataflow.analysis.RegularTransferResult;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.analysis.FlowExpressions.Receiver;
import org.checkerframework.dataflow.cfg.node.ClassNameNode;
import org.checkerframework.dataflow.cfg.node.IntegerLiteralNode;
import org.checkerframework.dataflow.cfg.node.MethodAccessNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.checker.regex.qual.Regex;
import org.checkerframework.framework.flow.CFAbstractTransfer;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.util.FlowExpressionParseUtil;
import org.checkerframework.framework.util.FlowExpressionParseUtil.FlowExpressionContext;
import org.checkerframework.framework.util.FlowExpressionParseUtil.FlowExpressionParseException;
import org.checkerframework.javacutil.AnnotationUtils;

public class RegexTransfer extends
        CFAbstractTransfer<CFValue, CFStore, RegexTransfer> {

    /** Like super.analysis, but more specific type. */
    protected RegexAnalysis analysis;

    public RegexTransfer(RegexAnalysis analysis) {
        super(analysis);
        this.analysis = analysis;
    }

    // TODO: These are special cases for isRegex(String, int) and asRegex(String, int).
    // They should be replaced by adding an @EnsuresQualifierIf annotation that supports
    // specifying attributes.
    @Override
    public TransferResult<CFValue, CFStore> visitMethodInvocation(
            MethodInvocationNode n, TransferInput<CFValue, CFStore> in) {
        RegexAnnotatedTypeFactory factory = (RegexAnnotatedTypeFactory) analysis
                .getTypeFactory();
        TransferResult<CFValue, CFStore> result = super.visitMethodInvocation(
                n, in);

        // refine result for some helper methods
        MethodAccessNode target = n.getTarget();
        ExecutableElement method = target.getMethod();
        Node receiver = target.getReceiver();
        if (receiver instanceof ClassNameNode) {
            ClassNameNode cn = (ClassNameNode) receiver;
            String receiverName = cn.getElement().toString();
            // RegexUtil.isRegex(s, groups) method
            // (No special case is needed for isRegex(String) because of
            // the annotatation on that method's definition.)
            if (isRegexUtil(receiverName)
                    && method.toString().equals(
                            "isRegex(java.lang.String,int)")) {
                CFStore thenStore = result.getRegularStore();
                CFStore elseStore = thenStore.copy();
                ConditionalTransferResult<CFValue, CFStore> newResult = new ConditionalTransferResult<>(
                        result.getResultValue(), thenStore, elseStore);
                FlowExpressionContext context = FlowExpressionParseUtil
                        .buildFlowExprContextForUse(n, factory.getContext());
                try {
                    Receiver firstParam = FlowExpressionParseUtil.parse(
                            "#1", context, factory.getPath(n.getTree()));
                    // add annotation with correct group count (if possible,
                    // regex annotation without count otherwise)
                    Node count = n.getArgument(1);
                    if (count instanceof IntegerLiteralNode) {
                        IntegerLiteralNode iln = (IntegerLiteralNode) count;
                        Integer groupCount = iln.getValue();
                        AnnotationMirror regexAnnotation = factory.createRegexAnnotation(groupCount);
                        thenStore.insertValue(firstParam, regexAnnotation);
                    } else {
                        AnnotationMirror regexAnnotation = AnnotationUtils
                                .fromClass(factory.getElementUtils(),
                                        Regex.class);
                        thenStore.insertValue(firstParam, regexAnnotation);
                    }
                } catch (FlowExpressionParseException e) {
                    assert false;
                }
                return newResult;
            }

            // RegexUtil.asRegex(s, groups) method
            // (No special case is needed for asRegex(String) because of
            // the annotatation on that method's definition.)
            if (isRegexUtil(receiverName)
                    && method.toString().equals(
                            "asRegex(java.lang.String,int)")) {
                // add annotation with correct group count (if possible,
                // regex annotation without count otherwise)
                AnnotationMirror regexAnnotation;
                Node count = n.getArgument(1);
                if (count instanceof IntegerLiteralNode) {
                    IntegerLiteralNode iln = (IntegerLiteralNode) count;
                    Integer groupCount = iln.getValue();
                    regexAnnotation = factory
                            .createRegexAnnotation(groupCount);
                } else {
                    regexAnnotation = AnnotationUtils.fromClass(
                            factory.getElementUtils(), Regex.class);
                }
                CFValue newResultValue = analysis
                        .createSingleAnnotationValue(regexAnnotation,
                                result.getResultValue().getType()
                                        .getUnderlyingType());
                return new RegularTransferResult<>(newResultValue,
                        result.getRegularStore());
            }
        }

        return result;
    };

    /**
     * Returns true if the given receiver is a class named "RegexUtil".
     */
    private boolean isRegexUtil(String receiver) {
        return receiver.equals("RegexUtil") || receiver.endsWith(".RegexUtil");
    }
}
