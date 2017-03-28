package org.checkerframework.common.value;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.common.value.qual.BoolVal;
import org.checkerframework.common.value.qual.BottomVal;
import org.checkerframework.common.value.qual.DoubleVal;
import org.checkerframework.common.value.qual.IntRange;
import org.checkerframework.common.value.qual.IntVal;
import org.checkerframework.common.value.qual.StringVal;
import org.checkerframework.common.value.qual.UnknownVal;
import org.checkerframework.common.value.util.NumberMath;
import org.checkerframework.common.value.util.NumberUtils;
import org.checkerframework.common.value.util.Range;
import org.checkerframework.dataflow.analysis.RegularTransferResult;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.BitwiseAndNode;
import org.checkerframework.dataflow.cfg.node.BitwiseComplementNode;
import org.checkerframework.dataflow.cfg.node.BitwiseOrNode;
import org.checkerframework.dataflow.cfg.node.BitwiseXorNode;
import org.checkerframework.dataflow.cfg.node.ConditionalAndNode;
import org.checkerframework.dataflow.cfg.node.ConditionalNotNode;
import org.checkerframework.dataflow.cfg.node.ConditionalOrNode;
import org.checkerframework.dataflow.cfg.node.EqualToNode;
import org.checkerframework.dataflow.cfg.node.FloatingDivisionNode;
import org.checkerframework.dataflow.cfg.node.FloatingRemainderNode;
import org.checkerframework.dataflow.cfg.node.GreaterThanNode;
import org.checkerframework.dataflow.cfg.node.GreaterThanOrEqualNode;
import org.checkerframework.dataflow.cfg.node.IntegerDivisionNode;
import org.checkerframework.dataflow.cfg.node.IntegerRemainderNode;
import org.checkerframework.dataflow.cfg.node.LeftShiftNode;
import org.checkerframework.dataflow.cfg.node.LessThanNode;
import org.checkerframework.dataflow.cfg.node.LessThanOrEqualNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.NotEqualNode;
import org.checkerframework.dataflow.cfg.node.NumericalAdditionNode;
import org.checkerframework.dataflow.cfg.node.NumericalMinusNode;
import org.checkerframework.dataflow.cfg.node.NumericalMultiplicationNode;
import org.checkerframework.dataflow.cfg.node.NumericalPlusNode;
import org.checkerframework.dataflow.cfg.node.NumericalSubtractionNode;
import org.checkerframework.dataflow.cfg.node.SignedRightShiftNode;
import org.checkerframework.dataflow.cfg.node.StringConcatenateAssignmentNode;
import org.checkerframework.dataflow.cfg.node.StringConcatenateNode;
import org.checkerframework.dataflow.cfg.node.StringConversionNode;
import org.checkerframework.dataflow.cfg.node.UnsignedRightShiftNode;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFTransfer;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TypesUtils;

public class ValueTransfer extends CFTransfer {
    ValueAnnotatedTypeFactory atypefactory;

    public ValueTransfer(CFAbstractAnalysis<CFValue, CFStore, CFTransfer> analysis) {
        super(analysis);
        atypefactory = (ValueAnnotatedTypeFactory) analysis.getTypeFactory();
    }

    /**
     * Returns a list of possible values for {@code subNode}, as casted to a String. Returns null if
     * {@code subNode}'s type is top/unknown. Returns an empty list if {@code subNode}'s type is
     * bottom.
     */
    private List<String> getStringValues(Node subNode, TransferInput<CFValue, CFStore> p) {
        CFValue value = p.getValueOfSubNode(subNode);
        // @StringVal, @UnknownVal, @BottomVal
        AnnotationMirror stringAnno =
                AnnotationUtils.getAnnotationByClass(value.getAnnotations(), StringVal.class);
        if (stringAnno != null) {
            return AnnotationUtils.getElementValueArray(stringAnno, "value", String.class, true);
        }
        AnnotationMirror topAnno =
                AnnotationUtils.getAnnotationByClass(value.getAnnotations(), UnknownVal.class);
        if (topAnno != null) {
            return null;
        }
        AnnotationMirror bottomAnno =
                AnnotationUtils.getAnnotationByClass(value.getAnnotations(), BottomVal.class);
        if (bottomAnno != null) {
            return new ArrayList<String>();
        }

        // @IntVal, @IntRange, @DoubleVal, @BoolVal (have to be converted to string)
        List<? extends Object> values;
        AnnotationMirror numberAnno =
                AnnotationUtils.getAnnotationByClass(value.getAnnotations(), BoolVal.class);
        if (numberAnno != null) {
            values = getBooleanValues(subNode, p);
        } else if (subNode.getType().getKind() == TypeKind.CHAR) {
            values = getCharValues(subNode, p);
        } else if (subNode instanceof StringConversionNode) {
            return getStringValues(((StringConversionNode) subNode).getOperand(), p);
        } else if (isIntRange(subNode, p)) {
            Range range = getIntRange(subNode, p);
            List<Long> longValues = ValueCheckerUtils.getValuesFromRange(range, Long.class);
            values = NumberUtils.castNumbers(subNode.getType(), longValues);
        } else {
            values = getNumericalValues(subNode, p);
        }
        if (values == null) {
            return null;
        }
        List<String> stringValues = new ArrayList<String>();
        for (Object o : values) {
            stringValues.add(o.toString());
        }
        // Empty list means bottom value
        return stringValues.isEmpty() ? Collections.singletonList("null") : stringValues;
    }

    /** Get possible boolean values from @BoolVal. */
    private List<Boolean> getBooleanValues(Node subNode, TransferInput<CFValue, CFStore> p) {
        CFValue value = p.getValueOfSubNode(subNode);
        AnnotationMirror intAnno =
                AnnotationUtils.getAnnotationByClass(value.getAnnotations(), BoolVal.class);
        return ValueAnnotatedTypeFactory.getBooleanValues(intAnno);
    }

    /** Get possible char values from annotation @IntRange or @IntVal. */
    private List<Character> getCharValues(Node subNode, TransferInput<CFValue, CFStore> p) {
        CFValue value = p.getValueOfSubNode(subNode);
        AnnotationMirror intAnno;

        intAnno = AnnotationUtils.getAnnotationByClass(value.getAnnotations(), IntVal.class);
        if (intAnno != null) {
            return ValueAnnotatedTypeFactory.getCharValues(intAnno);
        }

        intAnno = AnnotationUtils.getAnnotationByClass(value.getAnnotations(), IntRange.class);
        if (intAnno != null) {
            Range range = ValueAnnotatedTypeFactory.getIntRange(intAnno);
            return ValueCheckerUtils.getValuesFromRange(range, Character.class);
        }

        return new ArrayList<Character>();
    }

    /**
     * Returns a list of possible values, or null if no estimate is available and any value is
     * possible.
     */
    private List<? extends Number> getNumericalValues(
            Node subNode, TransferInput<CFValue, CFStore> p) {
        CFValue value = p.getValueOfSubNode(subNode);
        List<? extends Number> values = null;
        AnnotationMirror intValAnno =
                AnnotationUtils.getAnnotationByClass(value.getAnnotations(), IntVal.class);
        if (intValAnno != null) {
            values = AnnotationUtils.getElementValueArray(intValAnno, "value", Long.class, true);
        }
        AnnotationMirror doubleValAnno =
                AnnotationUtils.getAnnotationByClass(value.getAnnotations(), DoubleVal.class);
        if (doubleValAnno != null) {
            values =
                    AnnotationUtils.getElementValueArray(
                            doubleValAnno, "value", Double.class, true);
        }
        AnnotationMirror bottomValAnno =
                AnnotationUtils.getAnnotationByClass(value.getAnnotations(), BottomVal.class);
        if (bottomValAnno != null) {
            return new ArrayList<>();
        }
        if (values == null) {
            return null;
        }
        return NumberUtils.castNumbers(subNode.getType(), values);
    }

    /** Get possible integer range from annotation. */
    private Range getIntRange(Node subNode, TransferInput<CFValue, CFStore> p) {
        CFValue value = p.getValueOfSubNode(subNode);
        Range range = Range.EVERYTHING;
        AnnotationMirror intRangeAnno =
                AnnotationUtils.getAnnotationByClass(value.getAnnotations(), IntRange.class);
        if (intRangeAnno != null) {
            range = ValueAnnotatedTypeFactory.getIntRange(intRangeAnno);
        }
        AnnotationMirror intValAnno =
                AnnotationUtils.getAnnotationByClass(value.getAnnotations(), IntVal.class);
        if (intValAnno != null) {
            List<Long> values =
                    AnnotationUtils.getElementValueArray(intValAnno, "value", Long.class, true);
            range = ValueCheckerUtils.getRangeFromValues(values);
        }
        AnnotationMirror doubleValAnno =
                AnnotationUtils.getAnnotationByClass(value.getAnnotations(), DoubleVal.class);
        if (doubleValAnno != null) {
            List<Double> values =
                    AnnotationUtils.getElementValueArray(
                            doubleValAnno, "value", Double.class, true);
            range = ValueCheckerUtils.getRangeFromValues(values);
        }
        AnnotationMirror bottomValAnno =
                AnnotationUtils.getAnnotationByClass(value.getAnnotations(), BottomVal.class);
        if (bottomValAnno != null) {
            return Range.NOTHING;
        }
        return NumberUtils.castRange(subNode.getType(), range);
    }

    /** a helper function to determine if this node is annotated with @IntRange */
    private boolean isIntRange(Node subNode, TransferInput<CFValue, CFStore> p) {
        CFValue value = p.getValueOfSubNode(subNode);
        return AnnotationUtils.getAnnotationByClass(value.getAnnotations(), IntRange.class) != null;
    }

    /**
     * Create a new transfer result based on the original result and the new annotation.
     *
     * @param result the original result
     * @param resultAnno the new annotation
     * @return the new transfer result
     */
    private TransferResult<CFValue, CFStore> createNewResult(
            TransferResult<CFValue, CFStore> result, AnnotationMirror resultAnno) {
        CFValue newResultValue =
                analysis.createSingleAnnotationValue(
                        resultAnno, result.getResultValue().getUnderlyingType());
        return new RegularTransferResult<>(newResultValue, result.getRegularStore());
    }

    /** Create a boolean transfer result. */
    private TransferResult<CFValue, CFStore> createNewResultBoolean(
            TransferResult<CFValue, CFStore> result, List<Boolean> resultValues) {
        AnnotationMirror boolVal = atypefactory.createBooleanAnnotation(resultValues);
        return createNewResult(result, boolVal);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitStringConcatenateAssignment(
            StringConcatenateAssignmentNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<CFValue, CFStore> result = super.visitStringConcatenateAssignment(n, p);
        return stringConcatenation(n.getLeftOperand(), n.getRightOperand(), p, result);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitStringConcatenate(
            StringConcatenateNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<CFValue, CFStore> result = super.visitStringConcatenate(n, p);
        return stringConcatenation(n.getLeftOperand(), n.getRightOperand(), p, result);
    }

    public TransferResult<CFValue, CFStore> stringConcatenation(
            Node leftOperand,
            Node rightOperand,
            TransferInput<CFValue, CFStore> p,
            TransferResult<CFValue, CFStore> result) {
        List<String> lefts = getStringValues(leftOperand, p);
        List<String> rights = getStringValues(rightOperand, p);
        List<String> concat;
        if (lefts == null || rights == null) {
            concat = null;
        } else {
            concat = new ArrayList<>();
            if (lefts.isEmpty()) {
                lefts = Collections.singletonList("null");
            }
            if (rights.isEmpty()) {
                rights = Collections.singletonList("null");
            }
            for (String left : lefts) {
                for (String right : rights) {
                    concat.add(left + right);
                }
            }
        }
        AnnotationMirror stringVal = atypefactory.createStringAnnotation(concat);
        TypeMirror underlyingType = result.getResultValue().getUnderlyingType();
        CFValue newResultValue = analysis.createSingleAnnotationValue(stringVal, underlyingType);
        return new RegularTransferResult<>(newResultValue, result.getRegularStore());
    }

    /** binary operations that are analyzed by the value checker */
    enum NumericalBinaryOps {
        ADDITION,
        SUBTRACTION,
        DIVISION,
        REMAINDER,
        MULTIPLICATION,
        SHIFT_LEFT,
        SIGNED_SHIFT_RIGHT,
        UNSIGNED_SHIFT_RIGHT,
        BITWISE_AND,
        BITWISE_OR,
        BITWISE_XOR;
    }

    /**
     * Get the refined annotation after a numerical binary operation.
     *
     * @param leftNode the node that represents the left operand
     * @param rightNode the node that represents the right operand
     * @param op the operator type
     * @param p the transfer input
     * @return the result annotation mirror
     */
    private AnnotationMirror calculateNumericalBinaryOp(
            Node leftNode,
            Node rightNode,
            NumericalBinaryOps op,
            TransferInput<CFValue, CFStore> p) {
        if (!isIntRange(leftNode, p) && !isIntRange(rightNode, p)) {
            List<Number> resultValues = calculateValuesBinaryOp(leftNode, rightNode, op, p);
            return atypefactory.createNumberAnnotationMirror(resultValues);
        } else {
            Range resultRange = calculateRangeBinaryOp(leftNode, rightNode, op, p);
            return atypefactory.createIntRangeAnnotation(resultRange);
        }
    }

    /** Calculate the result range after a binary operation between two numerical type nodes */
    private Range calculateRangeBinaryOp(
            Node leftNode,
            Node rightNode,
            NumericalBinaryOps op,
            TransferInput<CFValue, CFStore> p) {
        if (TypesUtils.isIntegral(leftNode.getType())
                && TypesUtils.isIntegral(rightNode.getType())) {
            Range leftRange = getIntRange(leftNode, p);
            Range rightRange = getIntRange(rightNode, p);
            Range resultRange;
            switch (op) {
                case ADDITION:
                    resultRange = leftRange.plus(rightRange);
                    break;
                case SUBTRACTION:
                    resultRange = leftRange.minus(rightRange);
                    break;
                case MULTIPLICATION:
                    resultRange = leftRange.times(rightRange);
                    break;
                case DIVISION:
                    resultRange = leftRange.divide(rightRange);
                    break;
                case REMAINDER:
                    resultRange = leftRange.remainder(rightRange);
                    break;
                case SHIFT_LEFT:
                    resultRange = leftRange.shiftLeft(rightRange);
                    break;
                case SIGNED_SHIFT_RIGHT:
                    resultRange = leftRange.signedShiftRight(rightRange);
                    break;
                case UNSIGNED_SHIFT_RIGHT:
                    resultRange = leftRange.unsignedShiftRight(rightRange);
                    break;
                case BITWISE_AND:
                    resultRange = leftRange.bitwiseAnd(rightRange);
                    break;
                case BITWISE_OR:
                    resultRange = leftRange.bitwiseOr(rightRange);
                    break;
                case BITWISE_XOR:
                    resultRange = leftRange.bitwiseXor(rightRange);
                    break;
                default:
                    throw new UnsupportedOperationException();
            }
            // Any integral type with less than 32 bits would be promoted to 32-bit int type during operations.
            return leftNode.getType().getKind() == TypeKind.LONG
                            || rightNode.getType().getKind() == TypeKind.LONG
                    ? resultRange
                    : resultRange.intRange();
        } else {
            return Range.EVERYTHING;
        }
    }

    /** Calculate the possible values after a binary operation between two numerical type nodes */
    private List<Number> calculateValuesBinaryOp(
            Node leftNode,
            Node rightNode,
            NumericalBinaryOps op,
            TransferInput<CFValue, CFStore> p) {
        List<? extends Number> lefts = getNumericalValues(leftNode, p);
        List<? extends Number> rights = getNumericalValues(rightNode, p);
        if (lefts == null || rights == null) {
            return null;
        }
        List<Number> resultValues = new ArrayList<>();
        for (Number left : lefts) {
            NumberMath<?> nmLeft = NumberMath.getNumberMath(left);
            for (Number right : rights) {
                switch (op) {
                    case ADDITION:
                        resultValues.add(nmLeft.plus(right));
                        break;
                    case DIVISION:
                        resultValues.add(nmLeft.divide(right));
                        break;
                    case MULTIPLICATION:
                        resultValues.add(nmLeft.times(right));
                        break;
                    case REMAINDER:
                        resultValues.add(nmLeft.remainder(right));
                        break;
                    case SUBTRACTION:
                        resultValues.add(nmLeft.minus(right));
                        break;
                    case SHIFT_LEFT:
                        resultValues.add(nmLeft.shiftLeft(right));
                        break;
                    case SIGNED_SHIFT_RIGHT:
                        resultValues.add(nmLeft.signedShiftRight(right));
                        break;
                    case UNSIGNED_SHIFT_RIGHT:
                        resultValues.add(nmLeft.unsignedShiftRight(right));
                        break;
                    case BITWISE_AND:
                        resultValues.add(nmLeft.bitwiseAnd(right));
                        break;
                    case BITWISE_OR:
                        resultValues.add(nmLeft.bitwiseOr(right));
                        break;
                    case BITWISE_XOR:
                        resultValues.add(nmLeft.bitwiseXor(right));
                        break;
                    default:
                        throw new UnsupportedOperationException();
                }
            }
        }
        return resultValues;
    }

    @Override
    public TransferResult<CFValue, CFStore> visitNumericalAddition(
            NumericalAdditionNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<CFValue, CFStore> transferResult = super.visitNumericalAddition(n, p);
        AnnotationMirror resultAnno =
                calculateNumericalBinaryOp(
                        n.getLeftOperand(), n.getRightOperand(), NumericalBinaryOps.ADDITION, p);
        return createNewResult(transferResult, resultAnno);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitNumericalSubtraction(
            NumericalSubtractionNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<CFValue, CFStore> transferResult = super.visitNumericalSubtraction(n, p);
        AnnotationMirror resultAnno =
                calculateNumericalBinaryOp(
                        n.getLeftOperand(), n.getRightOperand(), NumericalBinaryOps.SUBTRACTION, p);
        return createNewResult(transferResult, resultAnno);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitNumericalMultiplication(
            NumericalMultiplicationNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<CFValue, CFStore> transferResult = super.visitNumericalMultiplication(n, p);
        AnnotationMirror resultAnno =
                calculateNumericalBinaryOp(
                        n.getLeftOperand(),
                        n.getRightOperand(),
                        NumericalBinaryOps.MULTIPLICATION,
                        p);
        return createNewResult(transferResult, resultAnno);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitIntegerDivision(
            IntegerDivisionNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<CFValue, CFStore> transferResult = super.visitIntegerDivision(n, p);
        AnnotationMirror resultAnno =
                calculateNumericalBinaryOp(
                        n.getLeftOperand(), n.getRightOperand(), NumericalBinaryOps.DIVISION, p);
        return createNewResult(transferResult, resultAnno);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitFloatingDivision(
            FloatingDivisionNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<CFValue, CFStore> transferResult = super.visitFloatingDivision(n, p);
        AnnotationMirror resultAnno =
                calculateNumericalBinaryOp(
                        n.getLeftOperand(), n.getRightOperand(), NumericalBinaryOps.DIVISION, p);
        return createNewResult(transferResult, resultAnno);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitIntegerRemainder(
            IntegerRemainderNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<CFValue, CFStore> transferResult = super.visitIntegerRemainder(n, p);
        AnnotationMirror resultAnno =
                calculateNumericalBinaryOp(
                        n.getLeftOperand(), n.getRightOperand(), NumericalBinaryOps.REMAINDER, p);
        return createNewResult(transferResult, resultAnno);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitFloatingRemainder(
            FloatingRemainderNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<CFValue, CFStore> transferResult = super.visitFloatingRemainder(n, p);
        AnnotationMirror resultAnno =
                calculateNumericalBinaryOp(
                        n.getLeftOperand(), n.getRightOperand(), NumericalBinaryOps.REMAINDER, p);
        return createNewResult(transferResult, resultAnno);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitLeftShift(
            LeftShiftNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<CFValue, CFStore> transferResult = super.visitLeftShift(n, p);
        AnnotationMirror resultAnno =
                calculateNumericalBinaryOp(
                        n.getLeftOperand(), n.getRightOperand(), NumericalBinaryOps.SHIFT_LEFT, p);
        return createNewResult(transferResult, resultAnno);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitSignedRightShift(
            SignedRightShiftNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<CFValue, CFStore> transferResult = super.visitSignedRightShift(n, p);
        AnnotationMirror resultAnno =
                calculateNumericalBinaryOp(
                        n.getLeftOperand(),
                        n.getRightOperand(),
                        NumericalBinaryOps.SIGNED_SHIFT_RIGHT,
                        p);
        return createNewResult(transferResult, resultAnno);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitUnsignedRightShift(
            UnsignedRightShiftNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<CFValue, CFStore> transferResult = super.visitUnsignedRightShift(n, p);
        AnnotationMirror resultAnno =
                calculateNumericalBinaryOp(
                        n.getLeftOperand(),
                        n.getRightOperand(),
                        NumericalBinaryOps.UNSIGNED_SHIFT_RIGHT,
                        p);
        return createNewResult(transferResult, resultAnno);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitBitwiseAnd(
            BitwiseAndNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<CFValue, CFStore> transferResult = super.visitBitwiseAnd(n, p);
        AnnotationMirror resultAnno =
                calculateNumericalBinaryOp(
                        n.getLeftOperand(), n.getRightOperand(), NumericalBinaryOps.BITWISE_AND, p);
        return createNewResult(transferResult, resultAnno);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitBitwiseOr(
            BitwiseOrNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<CFValue, CFStore> transferResult = super.visitBitwiseOr(n, p);
        AnnotationMirror resultAnno =
                calculateNumericalBinaryOp(
                        n.getLeftOperand(), n.getRightOperand(), NumericalBinaryOps.BITWISE_OR, p);
        return createNewResult(transferResult, resultAnno);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitBitwiseXor(
            BitwiseXorNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<CFValue, CFStore> transferResult = super.visitBitwiseXor(n, p);
        AnnotationMirror resultAnno =
                calculateNumericalBinaryOp(
                        n.getLeftOperand(), n.getRightOperand(), NumericalBinaryOps.BITWISE_XOR, p);
        return createNewResult(transferResult, resultAnno);
    }

    /** unary operations that are analyzed by the value checker */
    enum NumericalUnaryOps {
        PLUS,
        MINUS,
        BITWISE_COMPLEMENT;
    }

    /**
     * Get the refined annotation after a numerical unary operation.
     *
     * @param operand the node that represents the operand
     * @param op the operator type
     * @param p the transfer input
     * @return the result annotation mirror
     */
    private AnnotationMirror calculateNumericalUnaryOp(
            Node operand, NumericalUnaryOps op, TransferInput<CFValue, CFStore> p) {
        if (!isIntRange(operand, p)) {
            List<Number> resultValues = calculateValuesUnaryOp(operand, op, p);
            return atypefactory.createNumberAnnotationMirror(resultValues);
        } else {
            Range resultRange = calculateRangeUnaryOp(operand, op, p);
            return atypefactory.createIntRangeAnnotation(resultRange);
        }
    }

    /** Calculate the result range after a unary operation of a numerical type node */
    private Range calculateRangeUnaryOp(
            Node operand, NumericalUnaryOps op, TransferInput<CFValue, CFStore> p) {
        if (TypesUtils.isIntegral(operand.getType())) {
            Range range = getIntRange(operand, p);
            Range resultRange;
            switch (op) {
                case PLUS:
                    resultRange = range.unaryPlus();
                    break;
                case MINUS:
                    resultRange = range.unaryMinus();
                    break;
                case BITWISE_COMPLEMENT:
                    resultRange = range.bitwiseComplement();
                    break;
                default:
                    throw new UnsupportedOperationException();
            }
            // Any integral type with less than 32 bits would be promoted to 32-bit int type during operations.
            return operand.getType().getKind() == TypeKind.LONG
                    ? resultRange
                    : resultRange.intRange();
        } else {
            return Range.EVERYTHING;
        }
    }

    /** Calculate the possible values after a unary operation of a numerical type node */
    private List<Number> calculateValuesUnaryOp(
            Node operand, NumericalUnaryOps op, TransferInput<CFValue, CFStore> p) {
        List<? extends Number> lefts = getNumericalValues(operand, p);
        if (lefts == null) {
            return null;
        }
        List<Number> resultValues = new ArrayList<>();
        for (Number left : lefts) {
            NumberMath<?> nmLeft = NumberMath.getNumberMath(left);
            switch (op) {
                case PLUS:
                    resultValues.add(nmLeft.unaryPlus());
                    break;
                case MINUS:
                    resultValues.add(nmLeft.unaryMinus());
                    break;
                case BITWISE_COMPLEMENT:
                    resultValues.add(nmLeft.bitwiseComplement());
                    break;
                default:
                    throw new UnsupportedOperationException();
            }
        }
        return resultValues;
    }

    @Override
    public TransferResult<CFValue, CFStore> visitNumericalMinus(
            NumericalMinusNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<CFValue, CFStore> transferResult = super.visitNumericalMinus(n, p);
        AnnotationMirror resultAnno =
                calculateNumericalUnaryOp(n.getOperand(), NumericalUnaryOps.MINUS, p);
        return createNewResult(transferResult, resultAnno);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitNumericalPlus(
            NumericalPlusNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<CFValue, CFStore> transferResult = super.visitNumericalPlus(n, p);
        AnnotationMirror resultAnno =
                calculateNumericalUnaryOp(n.getOperand(), NumericalUnaryOps.PLUS, p);
        return createNewResult(transferResult, resultAnno);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitBitwiseComplement(
            BitwiseComplementNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<CFValue, CFStore> transferResult = super.visitBitwiseComplement(n, p);
        AnnotationMirror resultAnno =
                calculateNumericalUnaryOp(n.getOperand(), NumericalUnaryOps.BITWISE_COMPLEMENT, p);
        return createNewResult(transferResult, resultAnno);
    }

    enum ComparisonOperators {
        EQUAL,
        NOT_EQUAL,
        GREATER_THAN,
        GREATER_THAN_EQ,
        LESS_THAN,
        LESS_THAN_EQ;
    }

    private List<Boolean> calculateBinaryComparison(
            Node leftNode,
            Node rightNode,
            ComparisonOperators op,
            TransferInput<CFValue, CFStore> p) {
        List<Boolean> resultValues = new ArrayList<>();
        if (!isIntRange(leftNode, p) && !isIntRange(rightNode, p)) {
            // TODO:
            // Handle @IntRange annotation when the control flow refinement is implemented
            // typetools/checker-framework#1163
            List<? extends Number> lefts = getNumericalValues(leftNode, p);
            List<? extends Number> rights = getNumericalValues(rightNode, p);
            if (lefts == null || rights == null) {
                return null;
            }
            for (Number left : lefts) {
                NumberMath<?> nmLeft = NumberMath.getNumberMath(left);
                for (Number right : rights) {
                    switch (op) {
                        case EQUAL:
                            resultValues.add(nmLeft.equalTo(right));
                            break;
                        case GREATER_THAN:
                            resultValues.add(nmLeft.greaterThan(right));
                            break;
                        case GREATER_THAN_EQ:
                            resultValues.add(nmLeft.greaterThanEq(right));
                            break;
                        case LESS_THAN:
                            resultValues.add(nmLeft.lessThan(right));
                            break;
                        case LESS_THAN_EQ:
                            resultValues.add(nmLeft.lessThanEq(right));
                            break;
                        case NOT_EQUAL:
                            resultValues.add(nmLeft.notEqualTo(right));
                            break;
                        default:
                            throw new UnsupportedOperationException();
                    }
                }
            }
        }
        return resultValues;
    }

    @Override
    public TransferResult<CFValue, CFStore> visitLessThan(
            LessThanNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<CFValue, CFStore> transferResult = super.visitLessThan(n, p);
        List<Boolean> resultValues =
                calculateBinaryComparison(
                        n.getLeftOperand(), n.getRightOperand(), ComparisonOperators.LESS_THAN, p);
        return createNewResultBoolean(transferResult, resultValues);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitLessThanOrEqual(
            LessThanOrEqualNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<CFValue, CFStore> transferResult = super.visitLessThanOrEqual(n, p);
        List<Boolean> resultValues =
                calculateBinaryComparison(
                        n.getLeftOperand(),
                        n.getRightOperand(),
                        ComparisonOperators.LESS_THAN_EQ,
                        p);
        return createNewResultBoolean(transferResult, resultValues);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitGreaterThan(
            GreaterThanNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<CFValue, CFStore> transferResult = super.visitGreaterThan(n, p);
        List<Boolean> resultValues =
                calculateBinaryComparison(
                        n.getLeftOperand(),
                        n.getRightOperand(),
                        ComparisonOperators.GREATER_THAN,
                        p);
        return createNewResultBoolean(transferResult, resultValues);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitGreaterThanOrEqual(
            GreaterThanOrEqualNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<CFValue, CFStore> transferResult = super.visitGreaterThanOrEqual(n, p);
        List<Boolean> resultValues =
                calculateBinaryComparison(
                        n.getLeftOperand(),
                        n.getRightOperand(),
                        ComparisonOperators.GREATER_THAN_EQ,
                        p);
        return createNewResultBoolean(transferResult, resultValues);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitEqualTo(
            EqualToNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<CFValue, CFStore> transferResult = super.visitEqualTo(n, p);
        if (TypesUtils.isPrimitive(n.getLeftOperand().getType())
                || TypesUtils.isPrimitive(n.getRightOperand().getType())) {
            // At least one must be a primitive otherwise reference equality is used.
            List<Boolean> resultValues =
                    calculateBinaryComparison(
                            n.getLeftOperand(), n.getRightOperand(), ComparisonOperators.EQUAL, p);
            return createNewResultBoolean(transferResult, resultValues);
        }
        return super.visitEqualTo(n, p);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitNotEqual(
            NotEqualNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<CFValue, CFStore> transferResult = super.visitNotEqual(n, p);
        if (TypesUtils.isPrimitive(n.getLeftOperand().getType())
                || TypesUtils.isPrimitive(n.getRightOperand().getType())) {
            // At least one must be a primitive otherwise reference equality is
            // used.
            List<Boolean> resultValues =
                    calculateBinaryComparison(
                            n.getLeftOperand(),
                            n.getRightOperand(),
                            ComparisonOperators.NOT_EQUAL,
                            p);
            return createNewResultBoolean(transferResult, resultValues);
        }
        return super.visitNotEqual(n, p);
    }

    enum ConditionalOperators {
        NOT,
        OR,
        AND;
    }

    private static final List<Boolean> ALL_BOOLEANS =
            Arrays.asList(new Boolean[] {Boolean.TRUE, Boolean.FALSE});

    private List<Boolean> calculateConditionalOperator(
            Node leftNode,
            Node rightNode,
            ConditionalOperators op,
            TransferInput<CFValue, CFStore> p) {
        List<Boolean> lefts = getBooleanValues(leftNode, p);
        if (lefts == null) {
            lefts = ALL_BOOLEANS;
        }
        List<Boolean> resultValues = new ArrayList<>();
        List<Boolean> rights = null;
        if (rightNode != null) {
            rights = getBooleanValues(rightNode, p);
            if (rights == null) {
                rights = ALL_BOOLEANS;
            }
        }
        switch (op) {
            case NOT:
                for (Boolean left : lefts) {
                    resultValues.add(!left);
                }
                return resultValues;
            case OR:
                for (Boolean left : lefts) {
                    for (Boolean right : rights) {
                        resultValues.add(left || right);
                    }
                }
                return resultValues;
            case AND:
                for (Boolean left : lefts) {
                    for (Boolean right : rights) {
                        resultValues.add(left && right);
                    }
                }
                return resultValues;
        }
        throw new RuntimeException("Unrecognized conditional operator " + op);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitConditionalNot(
            ConditionalNotNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<CFValue, CFStore> transferResult = super.visitConditionalNot(n, p);
        List<Boolean> resultValues =
                calculateConditionalOperator(n.getOperand(), null, ConditionalOperators.NOT, p);
        return createNewResultBoolean(transferResult, resultValues);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitConditionalAnd(
            ConditionalAndNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<CFValue, CFStore> transferResult = super.visitConditionalAnd(n, p);
        List<Boolean> resultValues =
                calculateConditionalOperator(
                        n.getLeftOperand(), n.getRightOperand(), ConditionalOperators.AND, p);
        return createNewResultBoolean(transferResult, resultValues);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitConditionalOr(
            ConditionalOrNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<CFValue, CFStore> transferResult = super.visitConditionalOr(n, p);
        List<Boolean> resultValues =
                calculateConditionalOperator(
                        n.getLeftOperand(), n.getRightOperand(), ConditionalOperators.OR, p);
        return createNewResultBoolean(transferResult, resultValues);
    }
}
