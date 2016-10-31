package org.checkerframework.checker.regex;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.dataflow.analysis.ConditionalTransferResult;
import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.dataflow.analysis.FlowExpressions.Receiver;
import org.checkerframework.dataflow.analysis.RegularTransferResult;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.ClassNameNode;
import org.checkerframework.dataflow.cfg.node.GreaterThanNode;
import org.checkerframework.dataflow.cfg.node.GreaterThanOrEqualNode;
import org.checkerframework.dataflow.cfg.node.IntegerLiteralNode;
import org.checkerframework.dataflow.cfg.node.LessThanNode;
import org.checkerframework.dataflow.cfg.node.LessThanOrEqualNode;
import org.checkerframework.dataflow.cfg.node.MethodAccessNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFTransfer;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TypesUtils;

public class RegexTransfer extends CFTransfer {

    private static final String IS_REGEX_METHOD_NAME = "isRegex";
    private static final String AS_REGEX_METHOD_NAME = "asRegex";
    private static final String GROUP_COUNT_METHOD_NAME = "groupCount";

    public RegexTransfer(CFAbstractAnalysis<CFValue, CFStore, CFTransfer> analysis) {
        super(analysis);
    }

    // TODO: These are special cases for isRegex(String, int) and asRegex(String, int).
    // They should be replaced by adding an @EnsuresQualifierIf annotation that supports
    // specifying attributes.
    @Override
    public TransferResult<CFValue, CFStore> visitMethodInvocation(
            MethodInvocationNode n, TransferInput<CFValue, CFStore> in) {
        TransferResult<CFValue, CFStore> result = super.visitMethodInvocation(n, in);

        // refine result for some helper methods
        MethodAccessNode target = n.getTarget();
        ExecutableElement method = target.getMethod();
        Node receiver = target.getReceiver();
        if (receiver instanceof ClassNameNode) {
            ClassNameNode cnn = (ClassNameNode) receiver;
            String receiverName = cnn.getElement().toString();
            if (isRegexUtil(receiverName)) {
                result = handleRegexUtil(n, method, result);
            }
        }
        return result;
    }

    private TransferResult<CFValue, CFStore> handleRegexUtil(
            MethodInvocationNode n,
            ExecutableElement method,
            TransferResult<CFValue, CFStore> result) {
        RegexAnnotatedTypeFactory factory = (RegexAnnotatedTypeFactory) analysis.getTypeFactory();
        if (ElementUtils.matchesElement(method, IS_REGEX_METHOD_NAME, String.class, int.class)) {
            // RegexUtil.isRegex(s, groups) method
            // (No special case is needed for isRegex(String) because of
            // the annotation on that method's definition.)

            CFStore thenStore = result.getRegularStore();
            CFStore elseStore = thenStore.copy();
            ConditionalTransferResult<CFValue, CFStore> newResult =
                    new ConditionalTransferResult<>(result.getResultValue(), thenStore, elseStore);
            Receiver firstParam =
                    FlowExpressions.internalReprOf(
                            factory.getContext().getAnnotationProvider(), n.getArgument(0));

            // add annotation with correct group count (if possible,
            // regex annotation without count otherwise)
            Node count = n.getArgument(1);
            int groupCount;
            if (count instanceof IntegerLiteralNode) {
                IntegerLiteralNode iln = (IntegerLiteralNode) count;
                groupCount = iln.getValue();
            } else {
                groupCount = 0;
            }
            AnnotationMirror regexAnnotation = factory.createRegexAnnotation(groupCount);
            thenStore.insertValue(firstParam, regexAnnotation);
            return newResult;
        } else if (ElementUtils.matchesElement(
                method, AS_REGEX_METHOD_NAME, String.class, int.class)) {
            // RegexUtil.asRegex(s, groups) method
            // (No special case is needed for asRegex(String) because of
            // the annotation on that method's definition.)

            // add annotation with correct group count (if possible,
            // regex annotation without count otherwise)
            AnnotationMirror regexAnnotation;
            Node count = n.getArgument(1);
            int groupCount;
            if (count instanceof IntegerLiteralNode) {
                IntegerLiteralNode iln = (IntegerLiteralNode) count;
                groupCount = iln.getValue();
            } else {
                groupCount = 0;
            }
            regexAnnotation = factory.createRegexAnnotation(groupCount);

            CFValue newResultValue =
                    analysis.createSingleAnnotationValue(
                            regexAnnotation, result.getResultValue().getUnderlyingType());
            return new RegularTransferResult<>(newResultValue, result.getRegularStore());
        }
        return result;
    }

    @Override
    public TransferResult<CFValue, CFStore> visitLessThan(
            LessThanNode n, TransferInput<CFValue, CFStore> in) {
        // Look for: constant < mat.groupCount()
        // Make mat be @Regex(constant + 1)
        TransferResult<CFValue, CFStore> res = super.visitLessThan(n, in);
        return handleMatcherGroupCount(n.getRightOperand(), n.getLeftOperand(), false, in, res);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitLessThanOrEqual(
            LessThanOrEqualNode n, TransferInput<CFValue, CFStore> in) {
        // Look for: constant <= mat.groupCount()
        // Make mat be @Regex(constant)
        TransferResult<CFValue, CFStore> res = super.visitLessThanOrEqual(n, in);
        return handleMatcherGroupCount(n.getRightOperand(), n.getLeftOperand(), true, in, res);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitGreaterThan(
            GreaterThanNode n, TransferInput<CFValue, CFStore> in) {

        TransferResult<CFValue, CFStore> res = super.visitGreaterThan(n, in);
        return handleMatcherGroupCount(n.getLeftOperand(), n.getRightOperand(), false, in, res);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitGreaterThanOrEqual(
            GreaterThanOrEqualNode n, TransferInput<CFValue, CFStore> in) {
        // Look for: mat.groupCount() >= constant
        // Make mat be @Regex(constant)
        TransferResult<CFValue, CFStore> res = super.visitGreaterThanOrEqual(n, in);
        return handleMatcherGroupCount(n.getLeftOperand(), n.getRightOperand(), true, in, res);
    }

    /**
     * See whether possibleMatcher is a call of groupCount on a Matcher and possibleConstant is a
     * constant. If so, annotate the matcher as constant + 1 if !isAlsoEqual constant if isAlsoEqual
     *
     * @param possibleMatcher the Node that might be a call of Matcher.groupCount()
     * @param possibleConstant the Node that might be a constant
     * @param isAlsoEqual whether the comparison operation is strict or reflexive
     * @param in the TransferInput
     * @param resultIn TransferResult
     * @return the possibly refined output TransferResult
     */
    private TransferResult<CFValue, CFStore> handleMatcherGroupCount(
            Node possibleMatcher,
            Node possibleConstant,
            boolean isAlsoEqual,
            TransferInput<CFValue, CFStore> in,
            TransferResult<CFValue, CFStore> resultIn) {
        if (!(possibleMatcher instanceof MethodInvocationNode)) {
            return resultIn;
        }
        if (!(possibleConstant instanceof IntegerLiteralNode)) {
            return resultIn;
        }

        MethodAccessNode methodAccessNode = ((MethodInvocationNode) possibleMatcher).getTarget();
        ExecutableElement method = methodAccessNode.getMethod();
        Node receiver = methodAccessNode.getReceiver();

        if (!isMatcherGroupCountMethod(method, receiver)) {
            return resultIn;
        }

        Receiver matcherReceiver =
                FlowExpressions.internalReprOf(analysis.getTypeFactory(), receiver);

        IntegerLiteralNode iln = (IntegerLiteralNode) possibleConstant;
        int groupCount;
        if (isAlsoEqual) {
            groupCount = iln.getValue();
        } else {
            groupCount = iln.getValue() + 1;
        }

        CFStore thenStore = resultIn.getRegularStore();
        CFStore elseStore = thenStore.copy();
        ConditionalTransferResult<CFValue, CFStore> newResult =
                new ConditionalTransferResult<>(resultIn.getResultValue(), thenStore, elseStore);
        RegexAnnotatedTypeFactory factory = (RegexAnnotatedTypeFactory) analysis.getTypeFactory();

        AnnotationMirror regexAnnotation = factory.createRegexAnnotation(groupCount);
        thenStore.insertValue(matcherReceiver, regexAnnotation);

        return newResult;
    }

    private boolean isMatcherGroupCountMethod(ExecutableElement method, Node receiver) {
        if (ElementUtils.matchesElement(method, GROUP_COUNT_METHOD_NAME)) {
            TypeMirror matcherType = receiver.getType();
            if (matcherType.getKind() != TypeKind.DECLARED) {
                return false;
            }
            DeclaredType matcherDT = (DeclaredType) matcherType;
            return TypesUtils.getQualifiedName(matcherDT)
                    .contentEquals(java.util.regex.Matcher.class.getCanonicalName());
        }
        return false;
    }
    /** Returns true if the given receiver is a class named "RegexUtil". */
    private boolean isRegexUtil(String receiver) {
        return receiver.equals("RegexUtil") || receiver.endsWith(".RegexUtil");
    }
}
