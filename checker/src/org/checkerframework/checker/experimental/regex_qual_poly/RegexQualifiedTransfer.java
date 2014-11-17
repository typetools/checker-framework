package org.checkerframework.checker.experimental.regex_qual_poly;

import org.checkerframework.checker.experimental.regex_qual.Regex;
import org.checkerframework.checker.experimental.regex_qual.Regex.RegexVal;
import org.checkerframework.checker.regex.RegexTransfer;
import org.checkerframework.dataflow.analysis.ConditionalTransferResult;
import org.checkerframework.dataflow.analysis.FlowExpressions.Receiver;
import org.checkerframework.dataflow.analysis.RegularTransferResult;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.ClassNameNode;
import org.checkerframework.dataflow.cfg.node.IntegerLiteralNode;
import org.checkerframework.dataflow.cfg.node.MethodAccessNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.framework.util.FlowExpressionParseUtil;
import org.checkerframework.framework.util.FlowExpressionParseUtil.FlowExpressionContext;
import org.checkerframework.framework.util.FlowExpressionParseUtil.FlowExpressionParseException;
import org.checkerframework.qualframework.base.dataflow.QualAnalysis;
import org.checkerframework.qualframework.base.dataflow.QualStore;
import org.checkerframework.qualframework.base.dataflow.QualTransfer;
import org.checkerframework.qualframework.base.dataflow.QualValue;
import org.checkerframework.qualframework.poly.PolyQual.GroundQual;
import org.checkerframework.qualframework.poly.QualParams;

import javax.lang.model.element.ExecutableElement;

/**
 * A reimplemention of {@link RegexTransfer} using QualifiedTypeMirrors instead of AnnotatedTypeMirrors.
 */
public class RegexQualifiedTransfer extends QualTransfer<QualParams<Regex>> {

    public RegexQualifiedTransfer(QualAnalysis<QualParams<Regex>> analysis) {
        super(analysis);
    }

    @Override
    public TransferResult<QualValue<QualParams<Regex>>, QualStore<QualParams<Regex>>> visitMethodInvocation(
            MethodInvocationNode n, TransferInput<QualValue<QualParams<Regex>>, QualStore<QualParams<Regex>>> in) {

        TransferResult<QualValue<QualParams<Regex>>, QualStore<QualParams<Regex>>> result = super.visitMethodInvocation(n, in);

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
                QualStore<QualParams<Regex>> thenStore = result.getRegularStore();
                QualStore<QualParams<Regex>> elseStore = thenStore.copy();
                ConditionalTransferResult<QualValue<QualParams<Regex>>, QualStore<QualParams<Regex>>> newResult = new ConditionalTransferResult<>(
                        result.getResultValue(), thenStore, elseStore);
                FlowExpressionContext context = FlowExpressionParseUtil
                        .buildFlowExprContextForUse(n, analysis.getContext());
                try {
                    Receiver firstParam = FlowExpressionParseUtil.parse(
                            "#1", context, analysis.getContext().getTypeFactory().getPath(n.getTree()));
                    // add annotation with correct group count (if possible,
                    // regex annotation without count otherwise)
                    Node count = n.getArgument(1);
                    if (count instanceof IntegerLiteralNode) {
                        IntegerLiteralNode iln = (IntegerLiteralNode) count;
                        Integer groupCount = iln.getValue();
                        Regex regex = new RegexVal(groupCount);
                        thenStore.insertValue(firstParam, new QualParams<>(new GroundQual<>(regex)));
                    } else {
                        thenStore.insertValue(firstParam, new QualParams<>(new GroundQual<Regex>(new RegexVal(0))));
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
                QualParams<Regex> regex;
                Node count = n.getArgument(1);
                if (count instanceof IntegerLiteralNode) {
                    IntegerLiteralNode iln = (IntegerLiteralNode) count;
                    Integer groupCount = iln.getValue();
                    regex = new QualParams<>(new GroundQual<Regex>(new RegexVal(groupCount)));
                } else {
                    regex = new QualParams<>(new GroundQual<Regex>(new RegexVal(0)));
                }
                QualValue<QualParams<Regex>> newResultValue = analysis
                        .createSingleAnnotationValue(regex,
                        result.getResultValue().getType().getUnderlyingType().getOriginalType());
                return new RegularTransferResult<>(newResultValue,
                        result.getRegularStore());
            }
        }

        return result;
    }

    /**
     * Returns true if the given receiver is a class named "RegexUtil".
     */
    private boolean isRegexUtil(String receiver) {
        return receiver.equals("RegexUtil") || receiver.endsWith(".RegexUtil");
    }
}
