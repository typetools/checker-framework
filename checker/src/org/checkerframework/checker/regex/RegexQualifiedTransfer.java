package org.checkerframework.checker.regex;

import org.checkerframework.checker.experimental.regex_qual.Regex;
import org.checkerframework.checker.experimental.regex_qual.Regex.RegexVal;
import org.checkerframework.checker.regex.classic.RegexTransfer;
import org.checkerframework.dataflow.analysis.ConditionalTransferResult;
import org.checkerframework.dataflow.analysis.FlowExpressions.Receiver;
import org.checkerframework.dataflow.analysis.FlowExpressions.LocalVariable;
import org.checkerframework.dataflow.analysis.RegularTransferResult;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.ClassNameNode;
import org.checkerframework.dataflow.cfg.node.GreaterThanNode;
import org.checkerframework.dataflow.cfg.node.GreaterThanOrEqualNode;
import org.checkerframework.dataflow.cfg.node.IntegerLiteralNode;
import org.checkerframework.dataflow.cfg.node.LessThanNode;
import org.checkerframework.dataflow.cfg.node.LessThanOrEqualNode;
import org.checkerframework.dataflow.cfg.node.LocalVariableNode;
import org.checkerframework.dataflow.cfg.node.MethodAccessNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.util.FlowExpressionParseUtil;
import org.checkerframework.framework.util.FlowExpressionParseUtil.FlowExpressionContext;
import org.checkerframework.framework.util.FlowExpressionParseUtil.FlowExpressionParseException;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TypesUtils;
import org.checkerframework.qualframework.base.QualifiedTypeMirror;
import org.checkerframework.qualframework.base.dataflow.QualAnalysis;
import org.checkerframework.qualframework.base.dataflow.QualStore;
import org.checkerframework.qualframework.base.dataflow.QualTransfer;
import org.checkerframework.qualframework.base.dataflow.QualValue;
import org.checkerframework.qualframework.poly.PolyQual.GroundQual;
import org.checkerframework.qualframework.poly.QualParams;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

/**
 * A reimplementation of {@link RegexTransfer} using {@link QualifiedTypeMirror}s
 * instead of {@link AnnotatedTypeMirror}s.
 *
 */
// TODO: Add a more descriptive description.
// TODO: Use the Constant Value Checker instead of just allowing int literals.
public class RegexQualifiedTransfer extends QualTransfer<QualParams<Regex>> {

    private static final String IS_REGEX_METHOD_NAME = "isRegex";
    private static final String AS_REGEX_METHOD_NAME = "asRegex";
    private static final String GROUP_COUNT_METHOD_NAME = "groupCount";

    public RegexQualifiedTransfer(QualAnalysis<QualParams<Regex>> analysis) {
        super(analysis);
    }

    /**
     * {@inheritDoc}
     *
     * Handle invocations of {@link org.checkerframework.checker.regex.RegexUtil} methods.
     */
    @Override
    public TransferResult<QualValue<QualParams<Regex>>, QualStore<QualParams<Regex>>> visitMethodInvocation(
            MethodInvocationNode n, TransferInput<QualValue<QualParams<Regex>>, QualStore<QualParams<Regex>>> in) {

        TransferResult<QualValue<QualParams<Regex>>, QualStore<QualParams<Regex>>> result;
        result = super.visitMethodInvocation(n, in);

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

    /**
     * Handle invocations of isRegex and asRegex on RegexUtil.
     *
     * @param n The method invocation.
     * @param method The method element.
     * @param resultIn the input {@link org.checkerframework.dataflow.analysis.TransferResult}
     * @return the possibly refined output {@link org.checkerframework.dataflow.analysis.TransferResult}
     */
    private TransferResult<QualValue<QualParams<Regex>>, QualStore<QualParams<Regex>>> handleRegexUtil(
            MethodInvocationNode n, ExecutableElement method,
            TransferResult<QualValue<QualParams<Regex>>, QualStore<QualParams<Regex>>> resultIn) {
        if (ElementUtils.matchesElement(method, IS_REGEX_METHOD_NAME, String.class, int.class)) {
            // RegexUtil.isRegex(s, groups) method
            // (No special case is needed for isRegex(String) because of
            // the annotation on that method's definition.)

            QualStore<QualParams<Regex>> thenStore = resultIn.getRegularStore();
            QualStore<QualParams<Regex>> elseStore = thenStore.copy();
            ConditionalTransferResult<QualValue<QualParams<Regex>>, QualStore<QualParams<Regex>>> newResult =
                    new ConditionalTransferResult<>(resultIn.getResultValue(), thenStore, elseStore);
            FlowExpressionContext context = FlowExpressionParseUtil.buildFlowExprContextForUse(n,
                    analysis.getContext());
            Receiver firstParam;
            try {
                // TODO: is this the easiest way to do this?
                firstParam = FlowExpressionParseUtil.parse("#1", context,
                        analysis.getContext().getTypeFactory().getPath(n.getTree()));
            } catch (FlowExpressionParseException e) {
                firstParam = null;
                assert false;
            }

            // add annotation with correct group count (if possible,
            // regex annotation without count otherwise)
            int groupCount = determineIntValue(n.getArgument(1));
            Regex regex = new RegexVal(groupCount);
            thenStore.insertValue(firstParam, new QualParams<>(new GroundQual<>(regex)));
            return newResult;
        } else if (ElementUtils.matchesElement(method, AS_REGEX_METHOD_NAME, String.class, int.class)) {
            // RegexUtil.asRegex(s, groups) method
            // (No special case is needed for asRegex(String) because of
            // the annotation on that method's definition.)

            // add annotation with correct group count (if possible,
            // regex annotation without count otherwise)
            int groupCount = determineIntValue(n.getArgument(1));
            QualParams<Regex> regex = new QualParams<>(new GroundQual<Regex>(new RegexVal(groupCount)));
            QualValue<QualParams<Regex>> newResultValue = analysis.createSingleAnnotationValue(regex,
                    resultIn.getResultValue().getType().getUnderlyingType().getOriginalType());
            return new RegularTransferResult<>(newResultValue, resultIn.getRegularStore());
        }
        return resultIn;
    }

    /** Determine the int value of the given Node.
     *
     * @param num Input Node.
     * @return The int value of num. 0 if num is not an int literal.
     */

    private int determineIntValue(Node num) {
        int groupCount;
        if (num instanceof IntegerLiteralNode) {
            IntegerLiteralNode iln = (IntegerLiteralNode) num;
            groupCount = iln.getValue();
        } else {
            groupCount = 0;
        }
        return groupCount;
    }

    /**
     * Returns true if the given receiver is a class named "RegexUtil".
     */
    private boolean isRegexUtil(String receiver) {
        return receiver.equals("RegexUtil") || receiver.endsWith(".RegexUtil");
    }


    /**
     * {@inheritDoc}
     *
     * Look for: {@code constant < matcher.groupCount()} and, if found,
     * annotate {@code matcher} as {@code @Regex(constant + 1)}
     */
    @Override
    public
    TransferResult<QualValue<QualParams<Regex>>, QualStore<QualParams<Regex>>>
    visitLessThan(LessThanNode n,
            TransferInput<QualValue<QualParams<Regex>>, QualStore<QualParams<Regex>>> in) {
        TransferResult<QualValue<QualParams<Regex>>, QualStore<QualParams<Regex>>> res;
        res = super.visitLessThan(n, in);
        res = handleMatcherGroupCount(n.getRightOperand(), n.getLeftOperand(), false, in, res);
        return res;
    }

    /**
     * {@inheritDoc}
     *
     * Look for: {@code constant <= matcher.groupCount()} and, if found,
     * annotate {@code matcher} as {@code @Regex(constant)}
     */
    @Override
    public
    TransferResult<QualValue<QualParams<Regex>>, QualStore<QualParams<Regex>>>
    visitLessThanOrEqual(LessThanOrEqualNode n,
            TransferInput<QualValue<QualParams<Regex>>, QualStore<QualParams<Regex>>> in) {
        TransferResult<QualValue<QualParams<Regex>>, QualStore<QualParams<Regex>>> res;
        res = super.visitLessThanOrEqual(n, in);
        res = handleMatcherGroupCount(n.getRightOperand(), n.getLeftOperand(), true, in, res);
        return res;
    }

    /**
     * {@inheritDoc}
     *
     * Look for: {@code matcher.groupCount() > constant} and, if found,
     * annotate {@code matcher} as {@code @Regex(constant + 1)}
     */
    @Override
    public
    TransferResult<QualValue<QualParams<Regex>>, QualStore<QualParams<Regex>>>
    visitGreaterThan(GreaterThanNode n,
            TransferInput<QualValue<QualParams<Regex>>, QualStore<QualParams<Regex>>> in) {
        TransferResult<QualValue<QualParams<Regex>>, QualStore<QualParams<Regex>>> res;
        res = super.visitGreaterThan(n, in);
        res = handleMatcherGroupCount(n.getLeftOperand(), n.getRightOperand(), false, in, res);
        return res;
    }

    /**
     * {@inheritDoc}
     *
     * Look for: {@code matcher.groupCount() >= constant} and, if found,
     * annotate {@code matcher} as {@code @Regex(constant)}
     */
    @Override
    public
    TransferResult<QualValue<QualParams<Regex>>, QualStore<QualParams<Regex>>>
    visitGreaterThanOrEqual(GreaterThanOrEqualNode n,
            TransferInput<QualValue<QualParams<Regex>>, QualStore<QualParams<Regex>>> in) {
        TransferResult<QualValue<QualParams<Regex>>, QualStore<QualParams<Regex>>> res;
        res = super.visitGreaterThanOrEqual(n, in);
        res = handleMatcherGroupCount(n.getLeftOperand(), n.getRightOperand(), true, in, res);
        return res;
    }

    /**
     * See whether {@code possibleMatcher} is a call of {@link java.util.regex.Matcher#groupCount()} and
     * {@code possibleConstant} is an int literal.
     * If so, annotate the matcher as {@code @Regex(n)} with <br>
     * {@code n == constant + 1}   if {@code !isAlsoEqual} <br>
     * {@code n == constant}       if {@code isAlsoEqual}
     *
     * @param possibleMatcher the Node that might be a call of {@link java.util.regex.Matcher#groupCount()}
     * @param possibleConstant the Node that might be a constant
     * @param isAlsoEqual whether the comparison operation is strict or reflexive
     * @param in the input {@link org.checkerframework.dataflow.analysis.TransferResult}
     * @return the possibly refined output {@link org.checkerframework.dataflow.analysis.TransferResult}
     */
    private
    TransferResult<QualValue<QualParams<Regex>>, QualStore<QualParams<Regex>>>
    handleMatcherGroupCount(
            Node possibleMatcher, Node possibleConstant, boolean isAlsoEqual,
            TransferInput<QualValue<QualParams<Regex>>, QualStore<QualParams<Regex>>> in,
            TransferResult<QualValue<QualParams<Regex>>, QualStore<QualParams<Regex>>> resultIn) {
        if (!(possibleMatcher instanceof MethodInvocationNode)) {
            return resultIn;
        }
        if (!(possibleConstant instanceof IntegerLiteralNode)) {
            return resultIn;
        }

        MethodInvocationNode min = (MethodInvocationNode) possibleMatcher;
        MethodAccessNode target = min.getTarget();
        ExecutableElement method = target.getMethod();

        if (!ElementUtils.matchesElement(method, GROUP_COUNT_METHOD_NAME)) {
            return resultIn;
        }

        Node receiver = target.getReceiver();
        Receiver matcherReceiver;
        if (receiver instanceof LocalVariableNode) {
            matcherReceiver = new LocalVariable((LocalVariableNode) receiver);
        } else {
            // TODO: what other receiver types should we refine?
            // Is there a simpler way to get from Node to Receiver?
            // visitMethodInvocation uses FlowExpressionParseUtil.parse, but
            // I'm not sure what would be usable to get the receiver of a method call.
            matcherReceiver = null;
        }

        // Check matcher is of type java.util.regex.Matcher.
        {
            TypeMirror matcherType = matcherReceiver.getType();
            if (matcherType.getKind() != TypeKind.DECLARED) {
                return resultIn;
            }
            DeclaredType matcherDT = (DeclaredType) matcherType;
            if (!TypesUtils.getQualifiedName(matcherDT).contentEquals(java.util.regex.Matcher.class.getCanonicalName())) {
                return resultIn;
            }
        }

        IntegerLiteralNode iln = (IntegerLiteralNode) possibleConstant;
        int groupCount;
        if (isAlsoEqual) {
            groupCount = iln.getValue();
        } else {
            groupCount = iln.getValue() + 1;
        }

        QualStore<QualParams<Regex>> thenStore = resultIn.getRegularStore();
        QualStore<QualParams<Regex>> elseStore = thenStore.copy();
        ConditionalTransferResult<QualValue<QualParams<Regex>>, QualStore<QualParams<Regex>>> newResult =
                new ConditionalTransferResult<>(resultIn.getResultValue(), thenStore, elseStore);

        Regex regex = new RegexVal(groupCount);
        thenStore.insertValue(matcherReceiver, new QualParams<>(new GroundQual<>(regex)));

        return newResult;
    }

}
