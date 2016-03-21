package org.checkerframework.common.value;

import org.checkerframework.common.value.qual.BoolVal;
import org.checkerframework.common.value.qual.BottomVal;
import org.checkerframework.common.value.qual.DoubleVal;
import org.checkerframework.common.value.qual.IntVal;
import org.checkerframework.common.value.qual.StringVal;
import org.checkerframework.common.value.qual.UnknownVal;
import org.checkerframework.common.value.util.NumberMath;
import org.checkerframework.common.value.util.NumberUtils;
import org.checkerframework.dataflow.analysis.RegularTransferResult;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.*;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFTransfer;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TypesUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

public class ValueTransfer extends CFTransfer {
    AnnotatedTypeFactory atypefactory;

    public ValueTransfer(
            CFAbstractAnalysis<CFValue, CFStore, CFTransfer> analysis) {
        super(analysis);
        atypefactory = analysis.getTypeFactory();
    }

    private List<String> getStringValues(Node subNode,
            TransferInput<CFValue, CFStore> p) {
        CFValue value = p.getValueOfSubNode(subNode);
        // @StringVal, @BottomVal, @UnknownVal
        AnnotationMirror numberAnno = value.getType().getAnnotation(
                StringVal.class);
        if (numberAnno != null) {
            return AnnotationUtils.getElementValueArray(numberAnno, "value",
                    String.class, true);
        }
        numberAnno = value.getType().getAnnotation(UnknownVal.class);
        if (numberAnno != null) {
            return new ArrayList<String>();
        }
        numberAnno = value.getType().getAnnotation(BottomVal.class);
        if (numberAnno != null) {
            return Collections.singletonList("null");
        }

        //@IntVal, @DoubleVal, @BoolVal (have to be converted to string)
        List<? extends Object> values;
        numberAnno = value.getType().getAnnotation(BoolVal.class);
        if (numberAnno != null) {
            values = getBooleanValues(subNode, p);
        } else if (subNode.getType().getKind() == TypeKind.CHAR) {
            values = getCharValues(subNode, p);
        } else if (subNode instanceof StringConversionNode) {
           return getStringValues(
                    ((StringConversionNode) subNode).getOperand(), p);
        } else {
            values = getNumericalValues(subNode, p);
        }
        List<String> stringValues = new ArrayList<String>();
        for (Object o : values) {
            stringValues.add(o.toString());
        }
        return stringValues;
    }

    private List<Boolean> getBooleanValues(Node subNode,
            TransferInput<CFValue, CFStore> p) {
        CFValue value = p.getValueOfSubNode(subNode);
        AnnotationMirror intAnno = value.getType().getAnnotation(BoolVal.class);
        return ValueAnnotatedTypeFactory.getBooleanValues(intAnno);
    }

    private List<Character> getCharValues(Node subNode,
            TransferInput<CFValue, CFStore> p) {
        CFValue value = p.getValueOfSubNode(subNode);
        AnnotationMirror intAnno = value.getType().getAnnotation(IntVal.class);
        return ValueAnnotatedTypeFactory.getCharValues(intAnno);
    }

    private List<? extends Number> getNumericalValues(Node subNode,
            TransferInput<CFValue, CFStore> p) {
        CFValue value = p.getValueOfSubNode(subNode);
        AnnotationMirror numberAnno = value.getType().getAnnotation(
                IntVal.class);
        List<? extends Number> values;
        if (numberAnno == null) {
            numberAnno = value.getType().getAnnotation(DoubleVal.class);
            if (numberAnno != null) {
                values = AnnotationUtils.getElementValueArray(numberAnno,
                        "value", Double.class, true);
            } else {
                return new ArrayList<Number>();

            }
        } else {
            values = AnnotationUtils.getElementValueArray(numberAnno, "value",
                    Long.class, true);
        }
        return NumberUtils.castNumbers(subNode.getType(), values);
    }

    private AnnotationMirror createStringValAnnotationMirror(List<String> values) {
        if (values.isEmpty()) {
            return ((ValueAnnotatedTypeFactory) atypefactory).UNKNOWNVAL;
        }
        return ((ValueAnnotatedTypeFactory) atypefactory)
                .createStringAnnotation(values);
    }

    private AnnotationMirror createNumberAnnotationMirror(List<Number> values) {
        if (values.isEmpty()) {
            return ((ValueAnnotatedTypeFactory) atypefactory).UNKNOWNVAL;
        }
        Number first = values.get(0);
        if (first instanceof Integer || first instanceof Short
                || first instanceof Long) {
            List<Long> intValues = new ArrayList<>();
            for (Number number : values) {
                intValues.add(number.longValue());
            }
            return ((ValueAnnotatedTypeFactory) atypefactory)
                    .createIntValAnnotation(intValues);
        }
        if (first instanceof Double || first instanceof Float) {
            List<Double> intValues = new ArrayList<>();
            for (Number number : values) {
                intValues.add(number.doubleValue());
            }
            return ((ValueAnnotatedTypeFactory) atypefactory)
                    .createDoubleValAnnotation(intValues);
        }
        throw new UnsupportedOperationException();
    }

    private AnnotationMirror createBooleanAnnotationMirror(List<Boolean> values) {
        if (values.isEmpty()) {
            return ((ValueAnnotatedTypeFactory) atypefactory).UNKNOWNVAL;
        }
        return ((ValueAnnotatedTypeFactory) atypefactory)
                .createBooleanAnnotation(values);

    }

    private TransferResult<CFValue, CFStore> createNewResult(
            TransferResult<CFValue, CFStore> result, List<Number> resultValues) {
        AnnotationMirror stringVal = createNumberAnnotationMirror(resultValues);
        CFValue newResultValue = analysis.createSingleAnnotationValue(
                stringVal, result.getResultValue().getType()
                        .getUnderlyingType());
        return new RegularTransferResult<>(newResultValue,
                result.getRegularStore());
    }

    private TransferResult<CFValue, CFStore> createNewResultBoolean(
            TransferResult<CFValue, CFStore> result, List<Boolean> resultValues) {
        AnnotationMirror stringVal = createBooleanAnnotationMirror(resultValues);
        CFValue newResultValue = analysis.createSingleAnnotationValue(
                stringVal, result.getResultValue().getType()
                                 .getUnderlyingType());
        return new RegularTransferResult<>(newResultValue,
                result.getRegularStore());
    }

    @Override
    public TransferResult<CFValue, CFStore> visitStringConcatenateAssignment(StringConcatenateAssignmentNode n,
                                                                             TransferInput<CFValue, CFStore> p) {
        TransferResult<CFValue, CFStore> result = super.visitStringConcatenateAssignment(n, p);
        return stringConcatenation(n.getLeftOperand(), n.getRightOperand(), p, result);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitStringConcatenate(StringConcatenateNode n,
                                                                   TransferInput<CFValue, CFStore> p) {
        TransferResult<CFValue, CFStore> result = super.visitStringConcatenate(n, p);
        return stringConcatenation(n.getLeftOperand(), n.getRightOperand(), p, result);
    }

    public TransferResult<CFValue, CFStore> stringConcatenation(Node leftOperand, Node rightOperand,
                                                                TransferInput<CFValue, CFStore> p,
                                                                TransferResult<CFValue, CFStore> result) {
        List<String> lefts = getStringValues(leftOperand, p);
        List<String> rights = getStringValues(rightOperand, p);
        List<String> concat = new ArrayList<>();
        for (String left : lefts) {
            for (String right : rights) {
                concat.add(left + right);
            }
        }
        AnnotationMirror stringVal = createStringValAnnotationMirror(concat);
        TypeMirror underlyingType = result.getResultValue().getType().getUnderlyingType();
        CFValue newResultValue = analysis.createSingleAnnotationValue(stringVal, underlyingType);
        return new RegularTransferResult<>(newResultValue, result.getRegularStore());
    }

    enum NumbericalBinaryOps {
        ADDTION, SUBTRACTION, DIVISION, REMAINDER, MULPLICATION, SHIFT_LEFT, SIGNED_SHIFT_RIGHT, UNSIGNED_SHIFT_RIGHT, BITWISE_AND, BITWISE_OR, BITWISE_XOR;
    }

    private List<Number> calcutateNumericalBinaryOp(Node leftNode,
            Node rightNode, NumbericalBinaryOps op,
            TransferInput<CFValue, CFStore> p) {
        List<? extends Number> lefts = getNumericalValues(leftNode, p);
        List<? extends Number> rights = getNumericalValues(rightNode, p);
        List<Number> resultValues = new ArrayList<>();
        for (Number left : lefts) {
            NumberMath<?> nmLeft = NumberMath.getNumberMath(left);
            for (Number right : rights) {
                switch (op) {
                case ADDTION:
                    resultValues.add(nmLeft.plus(right));
                    break;
                case DIVISION:
                    resultValues.add(nmLeft.divide(right));
                    break;
                case MULPLICATION:
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
                    resultValues.add(nmLeft.signedSiftRight(right));
                    break;
                case UNSIGNED_SHIFT_RIGHT:
                    resultValues.add(nmLeft.unsignedSiftRight(right));
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
        TransferResult<CFValue, CFStore> transferResult = super
                .visitNumericalAddition(n, p);
        List<Number> resultValues = calcutateNumericalBinaryOp(
                n.getLeftOperand(), n.getRightOperand(),
                NumbericalBinaryOps.ADDTION, p);
        return createNewResult(transferResult, resultValues);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitNumericalSubtraction(
            NumericalSubtractionNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<CFValue, CFStore> transferResult = super
                .visitNumericalSubtraction(n, p);
        List<Number> resultValues = calcutateNumericalBinaryOp(
                n.getLeftOperand(), n.getRightOperand(),
                NumbericalBinaryOps.SUBTRACTION, p);
        return createNewResult(transferResult, resultValues);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitNumericalMultiplication(
            NumericalMultiplicationNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<CFValue, CFStore> transferResult = super
                .visitNumericalMultiplication(n, p);
        List<Number> resultValues = calcutateNumericalBinaryOp(
                n.getLeftOperand(), n.getRightOperand(),
                NumbericalBinaryOps.MULPLICATION, p);
        return createNewResult(transferResult, resultValues);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitIntegerDivision(
            IntegerDivisionNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<CFValue, CFStore> transferResult = super
                .visitIntegerDivision(n, p);
        List<Number> resultValues = calcutateNumericalBinaryOp(
                n.getLeftOperand(), n.getRightOperand(),
                NumbericalBinaryOps.DIVISION, p);
        return createNewResult(transferResult, resultValues);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitFloatingDivision(
            FloatingDivisionNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<CFValue, CFStore> transferResult = super
                .visitFloatingDivision(n, p);
        List<Number> resultValues = calcutateNumericalBinaryOp(
                n.getLeftOperand(), n.getRightOperand(),
                NumbericalBinaryOps.DIVISION, p);
        return createNewResult(transferResult, resultValues);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitIntegerRemainder(
            IntegerRemainderNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<CFValue, CFStore> transferResult = super
                .visitIntegerRemainder(n, p);
        List<Number> resultValues = calcutateNumericalBinaryOp(
                n.getLeftOperand(), n.getRightOperand(),
                NumbericalBinaryOps.REMAINDER, p);
        return createNewResult(transferResult, resultValues);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitFloatingRemainder(
            FloatingRemainderNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<CFValue, CFStore> transferResult = super
                .visitFloatingRemainder(n, p);
        List<Number> resultValues = calcutateNumericalBinaryOp(
                n.getLeftOperand(), n.getRightOperand(),
                NumbericalBinaryOps.REMAINDER, p);
        return createNewResult(transferResult, resultValues);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitLeftShift(LeftShiftNode n,
            TransferInput<CFValue, CFStore> p) {
        TransferResult<CFValue, CFStore> transferResult = super.visitLeftShift(
                n, p);
        List<Number> resultValues = calcutateNumericalBinaryOp(
                n.getLeftOperand(), n.getRightOperand(),
                NumbericalBinaryOps.SHIFT_LEFT, p);
        return createNewResult(transferResult, resultValues);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitSignedRightShift(
            SignedRightShiftNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<CFValue, CFStore> transferResult = super
                .visitSignedRightShift(n, p);
        List<Number> resultValues = calcutateNumericalBinaryOp(
                n.getLeftOperand(), n.getRightOperand(),
                NumbericalBinaryOps.SIGNED_SHIFT_RIGHT, p);
        return createNewResult(transferResult, resultValues);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitUnsignedRightShift(
            UnsignedRightShiftNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<CFValue, CFStore> transferResult = super
                .visitUnsignedRightShift(n, p);
        List<Number> resultValues = calcutateNumericalBinaryOp(
                n.getLeftOperand(), n.getRightOperand(),
                NumbericalBinaryOps.UNSIGNED_SHIFT_RIGHT, p);
        return createNewResult(transferResult, resultValues);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitBitwiseAnd(BitwiseAndNode n,
            TransferInput<CFValue, CFStore> p) {
        TransferResult<CFValue, CFStore> transferResult = super
                .visitBitwiseAnd(n, p);
        List<Number> resultValues = calcutateNumericalBinaryOp(
                n.getLeftOperand(), n.getRightOperand(),
                NumbericalBinaryOps.BITWISE_AND, p);
        return createNewResult(transferResult, resultValues);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitBitwiseOr(BitwiseOrNode n,
            TransferInput<CFValue, CFStore> p) {
        TransferResult<CFValue, CFStore> transferResult = super.visitBitwiseOr(
                n, p);
        List<Number> resultValues = calcutateNumericalBinaryOp(
                n.getLeftOperand(), n.getRightOperand(),
                NumbericalBinaryOps.BITWISE_OR, p);
        return createNewResult(transferResult, resultValues);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitBitwiseXor(BitwiseXorNode n,
            TransferInput<CFValue, CFStore> p) {
        TransferResult<CFValue, CFStore> transferResult = super
                .visitBitwiseXor(n, p);
        List<Number> resultValues = calcutateNumericalBinaryOp(
                n.getLeftOperand(), n.getRightOperand(),
                NumbericalBinaryOps.BITWISE_XOR, p);
        return createNewResult(transferResult, resultValues);
    }

    enum NumbericalUnaryOps {
        PLUS, MINUS, BITWISE_COMPLEMENT;
    }

    private List<Number> calcutateNumericalUnaryOp(Node operand,
            NumbericalUnaryOps op, TransferInput<CFValue, CFStore> p) {
        List<? extends Number> lefts = getNumericalValues(operand, p);
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
        TransferResult<CFValue, CFStore> transferResult = super
                .visitNumericalMinus(n, p);
        List<Number> resultValues = calcutateNumericalUnaryOp(n.getOperand(),
                NumbericalUnaryOps.MINUS, p);
        return createNewResult(transferResult, resultValues);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitNumericalPlus(
            NumericalPlusNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<CFValue, CFStore> transferResult = super
                .visitNumericalPlus(n, p);
        List<Number> resultValues = calcutateNumericalUnaryOp(n.getOperand(),
                NumbericalUnaryOps.PLUS, p);
        return createNewResult(transferResult, resultValues);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitBitwiseComplement(
            BitwiseComplementNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<CFValue, CFStore> transferResult = super
                .visitBitwiseComplement(n, p);
        List<Number> resultValues = calcutateNumericalUnaryOp(n.getOperand(),
                NumbericalUnaryOps.BITWISE_COMPLEMENT, p);
        return createNewResult(transferResult, resultValues);
    }

    enum ComparisonOperators {
        EQUAL, NOT_EQUAL, GREATER_THAN, GREATER_THAN_EQ, LESS_THAN, LESS_THAN_EQ;
    }

    private List<Boolean> calcutateBinaryComparison(Node leftNode,
            Node rightNode, ComparisonOperators op,
            TransferInput<CFValue, CFStore> p) {
        List<? extends Number> lefts = getNumericalValues(leftNode, p);
        List<? extends Number> rights = getNumericalValues(rightNode, p);
        List<Boolean> resultValues = new ArrayList<>();
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
        return resultValues;
    }

    @Override
    public TransferResult<CFValue, CFStore> visitLessThan(LessThanNode n,
            TransferInput<CFValue, CFStore> p) {
        TransferResult<CFValue, CFStore> transferResult = super.visitLessThan(
                n, p);
        List<Boolean> resultValues = calcutateBinaryComparison(
                n.getLeftOperand(), n.getRightOperand(),
                ComparisonOperators.LESS_THAN, p);
        return createNewResultBoolean(transferResult, resultValues);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitLessThanOrEqual(
            LessThanOrEqualNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<CFValue, CFStore> transferResult = super
                .visitLessThanOrEqual(n, p);
        List<Boolean> resultValues = calcutateBinaryComparison(
                n.getLeftOperand(), n.getRightOperand(),
                ComparisonOperators.LESS_THAN_EQ, p);
        return createNewResultBoolean(transferResult, resultValues);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitGreaterThan(GreaterThanNode n,
            TransferInput<CFValue, CFStore> p) {
        TransferResult<CFValue, CFStore> transferResult = super
                .visitGreaterThan(n, p);
        List<Boolean> resultValues = calcutateBinaryComparison(
                n.getLeftOperand(), n.getRightOperand(),
                ComparisonOperators.GREATER_THAN, p);
        return createNewResultBoolean(transferResult, resultValues);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitGreaterThanOrEqual(
            GreaterThanOrEqualNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<CFValue, CFStore> transferResult = super
                .visitGreaterThanOrEqual(n, p);
        List<Boolean> resultValues = calcutateBinaryComparison(
                n.getLeftOperand(), n.getRightOperand(),
                ComparisonOperators.GREATER_THAN_EQ, p);
        return createNewResultBoolean(transferResult, resultValues);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitEqualTo(EqualToNode n,
            TransferInput<CFValue, CFStore> p) {
        TransferResult<CFValue, CFStore> transferResult = super.visitEqualTo(n,
                p);
        if (TypesUtils.isPrimitive(n.getLeftOperand().getType())
                || TypesUtils.isPrimitive(n.getRightOperand().getType())) {
            //At least one must be a primitive otherwise reference equality is used.
            List<Boolean> resultValues = calcutateBinaryComparison(
                    n.getLeftOperand(), n.getRightOperand(),
                    ComparisonOperators.EQUAL, p);
            return createNewResultBoolean(transferResult, resultValues);
        }
        return super.visitEqualTo(n, p);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitNotEqual(NotEqualNode n,
            TransferInput<CFValue, CFStore> p) {
        TransferResult<CFValue, CFStore> transferResult = super.visitNotEqual(
                n, p);
        if (TypesUtils.isPrimitive(n.getLeftOperand().getType())
                || TypesUtils.isPrimitive(n.getRightOperand().getType())) {
            // At least one must be a primitive otherwise reference equality is
            // used.
            List<Boolean> resultValues = calcutateBinaryComparison(
                    n.getLeftOperand(), n.getRightOperand(),
                    ComparisonOperators.NOT_EQUAL, p);
            return createNewResultBoolean(transferResult, resultValues);
        }
        return super.visitNotEqual(n, p);
    }

    enum ConditionalOperators {
        NOT, OR, AND;
    }

    private List<Boolean> calcutateCondtionalOperator(Node leftNode,
            Node rightNode, ConditionalOperators op,
            TransferInput<CFValue, CFStore> p) {
        List<Boolean> lefts = getBooleanValues(leftNode, p);
        List<Boolean> resultValues = new ArrayList<>();
        List<Boolean> rights = new ArrayList<Boolean>();
        if (rightNode != null) {
            rights = getBooleanValues(rightNode, p);
        }
        switch (op) {
        case NOT:
            for (Boolean left : lefts) {
                resultValues.add(!left);
            }
            return resultValues;
        case OR:
            if (lefts.isEmpty() && rights.size() == 1) {
                if (rights.get(0)) {
                    // unknown || true == true
                    return rights;
                }
            }

            if (rights.isEmpty() && lefts.size() == 1) {
                if (lefts.get(0)) {
                    // true || unknown == true
                    return lefts;
                }
            }

            for (Boolean left : lefts) {
                for (Boolean right : rights) {
                    resultValues.add(left || right);
                }
            }
            return resultValues;
        case AND:
            if (lefts.isEmpty() && rights.size() == 1) {
                if (!rights.get(0)) {
                    // unknown && false == false
                    return rights;
                }
            }

            if (rights.isEmpty() && lefts.size() == 1) {
                if (!lefts.get(0)) {
                    // false && unknown == false
                    return lefts;
                }
            }

            for (Boolean left : lefts) {
                for (Boolean right : rights) {
                    resultValues.add(left && right);
                }
            }
            return resultValues;

        }

        return resultValues;
    }

    @Override
    public TransferResult<CFValue, CFStore> visitConditionalNot(
            ConditionalNotNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<CFValue, CFStore> transferResult = super
                .visitConditionalNot(n, p);
        List<Boolean> resultValues = calcutateCondtionalOperator(
                n.getOperand(), null, ConditionalOperators.NOT, p);
        return createNewResultBoolean(transferResult, resultValues);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitConditionalAnd(
            ConditionalAndNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<CFValue, CFStore> transferResult = super
                .visitConditionalAnd(n, p);
        List<Boolean> resultValues = calcutateCondtionalOperator(
                n.getLeftOperand(), n.getRightOperand(),
                ConditionalOperators.AND, p);
        return createNewResultBoolean(transferResult, resultValues);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitConditionalOr(
            ConditionalOrNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<CFValue, CFStore> transferResult = super
                .visitConditionalOr(n, p);
        List<Boolean> resultValues = calcutateCondtionalOperator(
                n.getLeftOperand(), n.getRightOperand(),
                ConditionalOperators.OR, p);
        return createNewResultBoolean(transferResult, resultValues);
    }
}
