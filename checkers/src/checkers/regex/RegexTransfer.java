package checkers.regex;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;

import checkers.flow.analysis.ConditionalTransferResult;
import checkers.flow.analysis.FlowExpressions.Receiver;
import checkers.flow.analysis.RegularTransferResult;
import checkers.flow.analysis.TransferInput;
import checkers.flow.analysis.TransferResult;
import checkers.flow.analysis.checkers.CFAbstractTransfer;
import checkers.flow.analysis.checkers.CFStore;
import checkers.flow.analysis.checkers.CFValue;
import checkers.flow.cfg.node.ClassNameNode;
import checkers.flow.cfg.node.IntegerLiteralNode;
import checkers.flow.cfg.node.MethodAccessNode;
import checkers.flow.cfg.node.MethodInvocationNode;
import checkers.flow.cfg.node.Node;
import checkers.flow.util.FlowExpressionParseUtil;
import checkers.flow.util.FlowExpressionParseUtil.FlowExpressionContext;
import checkers.flow.util.FlowExpressionParseUtil.FlowExpressionParseException;
import checkers.regex.quals.Regex;
import checkers.util.AnnotationUtils;

public class RegexTransfer extends
        CFAbstractTransfer<CFValue, CFStore, RegexTransfer> {

    /** Like super.analysis, but more specific type. */
    protected RegexAnalysis analysis;

    public RegexTransfer(RegexAnalysis analysis) {
        super(analysis);
        this.analysis = analysis;
    }

    // TODO: These are special cases for isRegex(String, int) and asRegex(String, int).
    // They should be replaced by adding an @EnsuresAnnotationIf annotation that supports
    // specifying attributes.
    @Override
    public TransferResult<CFValue, CFStore> visitMethodInvocation(
            MethodInvocationNode n, TransferInput<CFValue, CFStore> in) {
        RegexAnnotatedTypeFactory factory = (RegexAnnotatedTypeFactory) analysis
                .getFactory();
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
            if (isRegexUtil(receiverName)
                    && method.toString().equals(
                            "isRegex(java.lang.String,int)")) {
                CFStore thenStore = result.getRegularStore();
                CFStore elseStore = thenStore.copy();
                ConditionalTransferResult<CFValue, CFStore> newResult = new ConditionalTransferResult<>(
                        result.getResultValue(), thenStore, elseStore);
                FlowExpressionContext context = FlowExpressionParseUtil
                        .buildFlowExprContextForUse(n, factory);
                try {
                    Receiver firstParam = FlowExpressionParseUtil.parse(
                            "#1", context, factory.getPath(n.getTree()));
                    // add annotation with correct group count (if possible,
                    // regex annotation without count otherwise)
                    Node count = n.getArgument(1);
                    if (count instanceof IntegerLiteralNode) {
                        IntegerLiteralNode iln = (IntegerLiteralNode) count;
                        Integer groupCount = iln.getValue();
                        RegexAnnotatedTypeFactory f = (RegexAnnotatedTypeFactory) factory;
                        AnnotationMirror regexAnnotation = f
                                .createRegexAnnotation(groupCount);
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
