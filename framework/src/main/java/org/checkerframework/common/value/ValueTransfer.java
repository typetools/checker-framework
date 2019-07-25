package org.checkerframework.common.value;

import com.sun.source.tree.Tree;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.common.value.qual.ArrayLen;
import org.checkerframework.common.value.qual.ArrayLenRange;
import org.checkerframework.common.value.qual.BoolVal;
import org.checkerframework.common.value.qual.BottomVal;
import org.checkerframework.common.value.qual.DoubleVal;
import org.checkerframework.common.value.qual.IntVal;
import org.checkerframework.common.value.qual.StringVal;
import org.checkerframework.common.value.qual.UnknownVal;
import org.checkerframework.common.value.util.NumberMath;
import org.checkerframework.common.value.util.NumberUtils;
import org.checkerframework.common.value.util.Range;
import org.checkerframework.dataflow.analysis.ConditionalTransferResult;
import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.dataflow.analysis.FlowExpressions.Receiver;
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
import org.checkerframework.dataflow.cfg.node.FieldAccessNode;
import org.checkerframework.dataflow.cfg.node.FloatingDivisionNode;
import org.checkerframework.dataflow.cfg.node.FloatingRemainderNode;
import org.checkerframework.dataflow.cfg.node.GreaterThanNode;
import org.checkerframework.dataflow.cfg.node.GreaterThanOrEqualNode;
import org.checkerframework.dataflow.cfg.node.IntegerDivisionNode;
import org.checkerframework.dataflow.cfg.node.IntegerRemainderNode;
import org.checkerframework.dataflow.cfg.node.LeftShiftNode;
import org.checkerframework.dataflow.cfg.node.LessThanNode;
import org.checkerframework.dataflow.cfg.node.LessThanOrEqualNode;
import org.checkerframework.dataflow.cfg.node.MethodAccessNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
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
import org.checkerframework.dataflow.util.NodeUtils;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFAbstractStore;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFTransfer;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.TypesUtils;

/** The transfer class for the Value Checker. */
public class ValueTransfer extends CFTransfer {
    /** The Value type factory. */
    protected final ValueAnnotatedTypeFactory atypefactory;
    /** The Value qualifier hierarchy. */
    protected final QualifierHierarchy hierarchy;

    /** Create a new ValueTransfer. */
    public ValueTransfer(CFAbstractAnalysis<CFValue, CFStore, CFTransfer> analysis) {
        super(analysis);
        atypefactory = (ValueAnnotatedTypeFactory) analysis.getTypeFactory();
        hierarchy = atypefactory.getQualifierHierarchy();
    }

    /** Returns a range of possible lengths for an integer from a range, as casted to a String. */
    private Range getIntRangeStringLengthRange(Node subNode, TransferInput<CFValue, CFStore> p) {
        Range valueRange = getIntRange(subNode, p);

        // Get lengths of the bounds
        int fromLength = Long.toString(valueRange.from).length();
        int toLength = Long.toString(valueRange.to).length();

        int lowerLength = Math.min(fromLength, toLength);
        // In case the range contains 0, the minimum length is 1 even if both bounds are longer
        if (valueRange.contains(0)) {
            lowerLength = 1;
        }

        int upperLength = Math.max(fromLength, toLength);

        return new Range(lowerLength, upperLength);
    }

    /** Returns a range of possible lengths for {@code subNode}, as casted to a String. */
    private Range getStringLengthRange(Node subNode, TransferInput<CFValue, CFStore> p) {

        CFValue value = p.getValueOfSubNode(subNode);

        AnnotationMirror arrayLenRangeAnno =
                AnnotationUtils.getAnnotationByClass(value.getAnnotations(), ArrayLenRange.class);

        if (arrayLenRangeAnno != null) {
            return ValueAnnotatedTypeFactory.getRange(arrayLenRangeAnno);
        }

        // @BottomVal
        if (AnnotationUtils.containsSameByClass(value.getAnnotations(), BottomVal.class)) {
            return Range.NOTHING;
        }

        TypeKind subNodeTypeKind = subNode.getType().getKind();

        // handle values converted to string (ints, longs, longs with @IntRange)
        if (subNode instanceof StringConversionNode) {
            return getStringLengthRange(((StringConversionNode) subNode).getOperand(), p);
        } else if (isIntRange(subNode, p)) {
            return getIntRangeStringLengthRange(subNode, p);
        } else if (subNodeTypeKind == TypeKind.INT) {
            // ints are between 1 and 11 characters long
            return new Range(1, 11);
        } else if (subNodeTypeKind == TypeKind.LONG) {
            // longs are between 1 and 20 characters long
            return new Range(1, 20);
        }

        return new Range(0, Integer.MAX_VALUE);
    }

    /**
     * Returns a list of possible lengths for {@code subNode}, as casted to a String. Returns null
     * if {@code subNode}'s type is top/unknown. Returns an empty list if {@code subNode}'s type is
     * bottom.
     */
    private List<Integer> getStringLengths(Node subNode, TransferInput<CFValue, CFStore> p) {

        CFValue value = p.getValueOfSubNode(subNode);

        // @ArrayLen
        AnnotationMirror arrayLenAnno =
                AnnotationUtils.getAnnotationByClass(value.getAnnotations(), ArrayLen.class);

        if (arrayLenAnno != null) {
            return ValueAnnotatedTypeFactory.getArrayLength(arrayLenAnno);
        }

        // @BottomVal
        if (AnnotationUtils.containsSameByClass(value.getAnnotations(), BottomVal.class)) {
            return new ArrayList<>();
        }

        TypeKind subNodeTypeKind = subNode.getType().getKind();

        // handle values converted to string (characters, bytes, shorts, ints with @IntRange)
        if (subNode instanceof StringConversionNode) {
            return getStringLengths(((StringConversionNode) subNode).getOperand(), p);
        } else if (subNodeTypeKind == TypeKind.CHAR) {
            // characters always have length 1
            return Collections.singletonList(1);
        } else if (isIntRange(subNode, p)) {
            // Try to get a list of lengths from a range of integer values converted to string
            // @IntVal is not checked for, because if it is present, we would already have the
            // actual string values
            Range lengthRange = getIntRangeStringLengthRange(subNode, p);
            return ValueCheckerUtils.getValuesFromRange(lengthRange, Integer.class);
        } else if (subNodeTypeKind == TypeKind.BYTE) {
            // bytes are between 1 and 4 characters long
            return ValueCheckerUtils.getValuesFromRange(new Range(1, 4), Integer.class);
        } else if (subNodeTypeKind == TypeKind.SHORT) {
            // shorts are between 1 and 6 characters long
            return ValueCheckerUtils.getValuesFromRange(new Range(1, 6), Integer.class);
        } else {
            return null;
        }
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
            return ValueAnnotatedTypeFactory.getStringValues(stringAnno);
        }
        AnnotationMirror topAnno =
                AnnotationUtils.getAnnotationByClass(value.getAnnotations(), UnknownVal.class);
        if (topAnno != null) {
            return null;
        }
        AnnotationMirror bottomAnno =
                AnnotationUtils.getAnnotationByClass(value.getAnnotations(), BottomVal.class);
        if (bottomAnno != null) {
            return new ArrayList<>();
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
        List<String> stringValues = new ArrayList<>();
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

        if (atypefactory.isIntRange(value.getAnnotations())) {
            intAnno =
                    hierarchy.findAnnotationInHierarchy(
                            value.getAnnotations(), atypefactory.UNKNOWNVAL);
            Range range = ValueAnnotatedTypeFactory.getRange(intAnno);
            return ValueCheckerUtils.getValuesFromRange(range, Character.class);
        }

        return new ArrayList<>();
    }

    private AnnotationMirror getValueAnnotation(Node subNode, TransferInput<CFValue, CFStore> p) {
        CFValue value = p.getValueOfSubNode(subNode);
        return getValueAnnotation(value);
    }

    /**
     * Extract the Value Checker annotation from a CFValue object.
     *
     * @param cfValue a CFValue object
     * @return the Value Checker annotation within cfValue
     */
    private AnnotationMirror getValueAnnotation(CFValue cfValue) {
        return hierarchy.findAnnotationInHierarchy(
                cfValue.getAnnotations(), atypefactory.UNKNOWNVAL);
    }

    /**
     * Returns a list of possible values, or null if no estimate is available and any value is
     * possible.
     */
    private List<? extends Number> getNumericalValues(
            Node subNode, TransferInput<CFValue, CFStore> p) {
        AnnotationMirror valueAnno = getValueAnnotation(subNode, p);
        return getNumericalValues(subNode, valueAnno);
    }

    private List<? extends Number> getNumericalValues(Node subNode, AnnotationMirror valueAnno) {

        if (valueAnno == null || AnnotationUtils.areSameByClass(valueAnno, UnknownVal.class)) {
            return null;
        } else if (AnnotationUtils.areSameByClass(valueAnno, BottomVal.class)) {
            return new ArrayList<>();
        }
        List<? extends Number> values;
        if (AnnotationUtils.areSameByClass(valueAnno, IntVal.class)) {
            values = ValueAnnotatedTypeFactory.getIntValues(valueAnno);
        } else if (AnnotationUtils.areSameByClass(valueAnno, DoubleVal.class)) {
            values = ValueAnnotatedTypeFactory.getDoubleValues(valueAnno);
        } else {
            return null;
        }
        return NumberUtils.castNumbers(subNode.getType(), values);
    }

    /** Get possible integer range from annotation. */
    private Range getIntRange(Node subNode, TransferInput<CFValue, CFStore> p) {
        AnnotationMirror val = getValueAnnotation(subNode, p);
        return getIntRangeFromAnnotation(subNode, val);
    }

    private Range getIntRangeFromAnnotation(Node node, AnnotationMirror val) {
        Range range;
        if (val == null || AnnotationUtils.areSameByClass(val, UnknownVal.class)) {
            range = Range.EVERYTHING;
        } else if (atypefactory.isIntRange(val)) {
            range = ValueAnnotatedTypeFactory.getRange(val);
        } else if (AnnotationUtils.areSameByClass(val, IntVal.class)) {
            List<Long> values = ValueAnnotatedTypeFactory.getIntValues(val);
            range = ValueCheckerUtils.getRangeFromValues(values);
        } else if (AnnotationUtils.areSameByClass(val, DoubleVal.class)) {
            List<Double> values = ValueAnnotatedTypeFactory.getDoubleValues(val);
            range = ValueCheckerUtils.getRangeFromValues(values);
        } else if (AnnotationUtils.areSameByClass(val, BottomVal.class)) {
            return Range.NOTHING;
        } else {
            range = Range.EVERYTHING;
        }
        return NumberUtils.castRange(node.getType(), range);
    }

    /** Returns true if this node is annotated with {@code @IntRange}. */
    private boolean isIntRange(Node subNode, TransferInput<CFValue, CFStore> p) {
        CFValue value = p.getValueOfSubNode(subNode);
        return atypefactory.isIntRange(value.getAnnotations());
    }

    /** Returns true if this node is annotated with {@code @UnknownVal}. */
    private boolean isIntegralUnknownVal(Node node, AnnotationMirror anno) {
        return AnnotationUtils.areSameByClass(anno, UnknownVal.class)
                && TypesUtils.isIntegral(node.getType());
    }

    /** Returns true if this node is annotated with {@code @IntRange} or {@code @UnknownVal}. */
    private boolean isIntRangeOrIntegralUnknownVal(Node node, TransferInput<CFValue, CFStore> p) {
        AnnotationMirror anno = getValueAnnotation(p.getValueOfSubNode(node));
        return isIntRange(node, p) || isIntegralUnknownVal(node, anno);
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
            CFStore thenStore,
            CFStore elseStore,
            List<Boolean> resultValues,
            TypeMirror underlyingType) {
        AnnotationMirror boolVal = atypefactory.createBooleanAnnotation(resultValues);
        CFValue newResultValue = analysis.createSingleAnnotationValue(boolVal, underlyingType);
        if (elseStore != null) {
            return new ConditionalTransferResult<>(newResultValue, thenStore, elseStore);
        } else {
            return new RegularTransferResult<>(newResultValue, thenStore);
        }
    }

    @Override
    public TransferResult<CFValue, CFStore> visitFieldAccess(
            FieldAccessNode node, TransferInput<CFValue, CFStore> in) {

        TransferResult<CFValue, CFStore> result = super.visitFieldAccess(node, in);
        refineArrayAtLengthAccess(node, result.getRegularStore());
        return result;
    }

    @Override
    public TransferResult<CFValue, CFStore> visitMethodInvocation(
            MethodInvocationNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<CFValue, CFStore> result = super.visitMethodInvocation(n, p);
        refineStringAtLengthInvocation(n, result.getRegularStore());
        return result;
    }

    /**
     * If array.length is encountered, transform its @IntVal annotation into an @ArrayLen annotation
     * for array.
     */
    private void refineArrayAtLengthAccess(FieldAccessNode arrayLengthNode, CFStore store) {
        if (!NodeUtils.isArrayLengthFieldAccess(arrayLengthNode)) {
            return;
        }

        refineAtLengthAccess(arrayLengthNode, arrayLengthNode.getReceiver(), store);
    }

    /**
     * If string.length() is encountered, transform its @IntVal annotation into an @ArrayLen
     * annotation for string.
     */
    private void refineStringAtLengthInvocation(
            MethodInvocationNode stringLengthNode, CFStore store) {
        MethodAccessNode methodAccessNode = stringLengthNode.getTarget();

        if (atypefactory.getMethodIdentifier().isStringLengthMethod(methodAccessNode.getMethod())) {
            refineAtLengthAccess(stringLengthNode, methodAccessNode.getReceiver(), store);
        }
    }

    /** Gets a value checker annotation relevant for an array or a string. */
    private AnnotationMirror getArrayOrStringAnnotation(Node arrayOrStringNode) {
        AnnotationMirror arrayOrStringAnno =
                atypefactory.getAnnotationMirror(arrayOrStringNode.getTree(), StringVal.class);
        if (arrayOrStringAnno == null) {
            arrayOrStringAnno =
                    atypefactory.getAnnotationMirror(arrayOrStringNode.getTree(), ArrayLen.class);
        }
        if (arrayOrStringAnno == null) {
            arrayOrStringAnno =
                    atypefactory.getAnnotationMirror(
                            arrayOrStringNode.getTree(), ArrayLenRange.class);
        }

        return arrayOrStringAnno;
    }

    /**
     * Transform @IntVal or @IntRange annotations of a array or string length into an @ArrayLen
     * or @ArrayLenRange annotation for the array or string.
     */
    private void refineAtLengthAccess(Node lengthNode, Node receiverNode, CFStore store) {
        Receiver lengthRec = FlowExpressions.internalReprOf(analysis.getTypeFactory(), lengthNode);

        // If the expression is not representable (for example if String.length() for some reason is
        // not marked @Pure, then do not refine.
        if (lengthRec instanceof FlowExpressions.Unknown) {
            return;
        }

        CFValue value = store.getValue(lengthRec);
        if (value == null) {
            return;
        }

        AnnotationMirror lengthAnno = getValueAnnotation(value);
        if (lengthAnno == null) {
            return;
        }
        if (AnnotationUtils.areSameByClass(lengthAnno, BottomVal.class)) {
            // If the length is bottom, then this is dead code, so the receiver type
            // should also be bottom.
            Receiver receiver = FlowExpressions.internalReprOf(atypefactory, receiverNode);
            store.insertValue(receiver, lengthAnno);
            return;
        }

        RangeOrListOfValues rolv;
        if (atypefactory.isIntRange(lengthAnno)) {
            rolv = new RangeOrListOfValues(ValueAnnotatedTypeFactory.getRange(lengthAnno));
        } else if (AnnotationUtils.areSameByClass(lengthAnno, IntVal.class)) {
            List<Long> lengthValues = ValueAnnotatedTypeFactory.getIntValues(lengthAnno);
            rolv = new RangeOrListOfValues(RangeOrListOfValues.convertLongsToInts(lengthValues));
        } else {
            return;
        }
        AnnotationMirror newRecAnno = rolv.createAnnotation(atypefactory);
        AnnotationMirror oldRecAnno = getArrayOrStringAnnotation(receiverNode);

        AnnotationMirror combinedRecAnno;
        // If the receiver doesn't have an @ArrayLen annotation, use the new annotation.
        // If it does have an annotation, combine the facts known about the receiver
        // with the facts known about its length using GLB.
        if (oldRecAnno == null) {
            combinedRecAnno = newRecAnno;
        } else {
            combinedRecAnno = hierarchy.greatestLowerBound(oldRecAnno, newRecAnno);
        }
        Receiver receiver = FlowExpressions.internalReprOf(analysis.getTypeFactory(), receiverNode);
        store.insertValue(receiver, combinedRecAnno);
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

    /**
     * Calculates possible lengths of a result of string concatenation of strings with known
     * lengths.
     */
    private List<Integer> calculateLengthAddition(
            List<Integer> leftLengths, List<Integer> rightLengths) {
        ArrayList<Integer> result = new ArrayList<>();

        for (int left : leftLengths) {
            for (int right : rightLengths) {
                long resultLength = (long) left + right;
                // Lengths not fitting into int are not allowed
                if (resultLength <= Integer.MAX_VALUE) {
                    result.add((int) resultLength);
                }
            }
        }

        return result;
    }

    /**
     * Calculates a range of possible lengths of a result of string concatenation of strings with
     * known ranges of lengths.
     */
    private Range calculateLengthRangeAddition(Range leftLengths, Range rightLengths) {
        return leftLengths.plus(rightLengths).intersect(Range.INT_EVERYTHING);
    }

    /** Creates an annotation for a result of string concatenation. */
    private AnnotationMirror createAnnotationForStringConcatenation(
            Node leftOperand, Node rightOperand, TransferInput<CFValue, CFStore> p) {

        // Try using sets of string values
        List<String> leftValues = getStringValues(leftOperand, p);
        List<String> rightValues = getStringValues(rightOperand, p);

        if (leftValues != null && rightValues != null) {
            // Both operands have known string values, compute set of results
            List<String> concatValues = new ArrayList<>();
            if (leftValues.isEmpty()) {
                leftValues = Collections.singletonList("null");
            }
            if (rightValues.isEmpty()) {
                rightValues = Collections.singletonList("null");
            }
            for (String left : leftValues) {
                for (String right : rightValues) {
                    concatValues.add(left + right);
                }
            }
            return atypefactory.createStringAnnotation(concatValues);
        }

        // Try using sets of lengths
        List<Integer> leftLengths =
                leftValues != null
                        ? ValueCheckerUtils.getLengthsForStringValues(leftValues)
                        : getStringLengths(leftOperand, p);
        List<Integer> rightLengths =
                rightValues != null
                        ? ValueCheckerUtils.getLengthsForStringValues(rightValues)
                        : getStringLengths(rightOperand, p);

        if (leftLengths != null && rightLengths != null) {
            // Both operands have known lengths, compute set of result lengths
            List<Integer> concatLengths = calculateLengthAddition(leftLengths, rightLengths);
            return atypefactory.createArrayLenAnnotation(concatLengths);
        }

        // Try using ranges of lengths
        Range leftLengthRange =
                leftLengths != null
                        ? ValueCheckerUtils.getRangeFromValues(leftLengths)
                        : getStringLengthRange(leftOperand, p);
        Range rightLengthRange =
                rightLengths != null
                        ? ValueCheckerUtils.getRangeFromValues(rightLengths)
                        : getStringLengthRange(rightOperand, p);

        if (leftLengthRange != null && rightLengthRange != null) {
            // Both operands have a length from a known range, compute a range of result lengths
            Range concatLengthRange =
                    calculateLengthRangeAddition(leftLengthRange, rightLengthRange);
            return atypefactory.createArrayLenRangeAnnotation(concatLengthRange);
        }

        return atypefactory.UNKNOWNVAL;
    }

    public TransferResult<CFValue, CFStore> stringConcatenation(
            Node leftOperand,
            Node rightOperand,
            TransferInput<CFValue, CFStore> p,
            TransferResult<CFValue, CFStore> result) {

        AnnotationMirror resultAnno =
                createAnnotationForStringConcatenation(leftOperand, rightOperand, p);

        TypeMirror underlyingType = result.getResultValue().getUnderlyingType();
        CFValue newResultValue = analysis.createSingleAnnotationValue(resultAnno, underlyingType);
        return new RegularTransferResult<>(newResultValue, result.getRegularStore());
    }

    /** Binary operations that are analyzed by the value checker. */
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
        if (!isIntRangeOrIntegralUnknownVal(leftNode, p)
                && !isIntRangeOrIntegralUnknownVal(rightNode, p)) {
            List<Number> resultValues = calculateValuesBinaryOp(leftNode, rightNode, op, p);
            return atypefactory.createNumberAnnotationMirror(resultValues);
        } else {
            Range resultRange = calculateRangeBinaryOp(leftNode, rightNode, op, p);
            return atypefactory.createIntRangeAnnotation(resultRange);
        }
    }

    /** Calculate the result range after a binary operation between two numerical type nodes. */
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
                    throw new BugInCF("ValueTransfer: unsupported operation: " + op);
            }
            // Any integral type with less than 32 bits would be promoted to 32-bit int type during
            // operations.
            return leftNode.getType().getKind() == TypeKind.LONG
                            || rightNode.getType().getKind() == TypeKind.LONG
                    ? resultRange
                    : resultRange.intRange();
        } else {
            return Range.EVERYTHING;
        }
    }

    /** Calculate the possible values after a binary operation between two numerical type nodes. */
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
                        Number result = nmLeft.divide(right);
                        if (result != null) {
                            resultValues.add(result);
                        }
                        break;
                    case MULTIPLICATION:
                        resultValues.add(nmLeft.times(right));
                        break;
                    case REMAINDER:
                        Number resultR = nmLeft.remainder(right);
                        if (resultR != null) {
                            resultValues.add(resultR);
                        }
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
                        throw new BugInCF("ValueTransfer: unsupported operation: " + op);
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

    /** Unary operations that are analyzed by the value checker. */
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

    /** Calculate the result range after a unary operation of a numerical type node. */
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
                    throw new BugInCF("ValueTransfer: unsupported operation: " + op);
            }
            // Any integral type with less than 32 bits would be promoted to 32-bit int type during
            // operations.
            return operand.getType().getKind() == TypeKind.LONG
                    ? resultRange
                    : resultRange.intRange();
        } else {
            return Range.EVERYTHING;
        }
    }

    /** Calculate the possible values after a unary operation of a numerical type node. */
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
                    throw new BugInCF("ValueTransfer: unsupported operation: " + op);
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
            CFValue leftValue,
            Node rightNode,
            CFValue rightValue,
            ComparisonOperators op,
            CFStore thenStore,
            CFStore elseStore) {
        AnnotationMirror leftAnno = getValueAnnotation(leftValue);
        AnnotationMirror rightAnno = getValueAnnotation(rightValue);

        if (atypefactory.isIntRange(leftAnno)
                || atypefactory.isIntRange(rightAnno)
                || isIntegralUnknownVal(rightNode, rightAnno)
                || isIntegralUnknownVal(leftNode, leftAnno)) {
            // If either is @UnknownVal, then refineIntRanges will treat it as the max range and
            // thus refine it if possible.  Also, if either is an @IntVal, then it will be
            // converted to a range.  This is less precise in some cases, but avoids the
            // complexity of comparing a list of values to a range. (This could be implemented in
            // the future.)
            return refineIntRanges(
                    leftNode, leftAnno, rightNode, rightAnno, op, thenStore, elseStore);
        }
        // This is a list of all the values that the expression can evaluate to.
        List<Boolean> resultValues = new ArrayList<>();

        List<? extends Number> lefts = getNumericalValues(leftNode, leftAnno);
        List<? extends Number> rights = getNumericalValues(rightNode, rightAnno);

        if (lefts == null || rights == null) {
            // Appropriately handle bottom when something is compared to bottom.
            if (AnnotationUtils.areSame(leftAnno, atypefactory.BOTTOMVAL)
                    || AnnotationUtils.areSame(rightAnno, atypefactory.BOTTOMVAL)) {
                return new ArrayList<>();
            }
            return null;
        }

        // These lists are used to refine the values in the store based on the results of the
        // comparison.
        List<Number> thenLeftVals = new ArrayList<>();
        List<Number> elseLeftVals = new ArrayList<>();
        List<Number> thenRightVals = new ArrayList<>();
        List<Number> elseRightVals = new ArrayList<>();

        for (Number left : lefts) {
            NumberMath<?> nmLeft = NumberMath.getNumberMath(left);
            for (Number right : rights) {
                Boolean result;
                switch (op) {
                    case EQUAL:
                        result = nmLeft.equalTo(right);
                        break;
                    case GREATER_THAN:
                        result = nmLeft.greaterThan(right);
                        break;
                    case GREATER_THAN_EQ:
                        result = nmLeft.greaterThanEq(right);
                        break;
                    case LESS_THAN:
                        result = nmLeft.lessThan(right);
                        break;
                    case LESS_THAN_EQ:
                        result = nmLeft.lessThanEq(right);
                        break;
                    case NOT_EQUAL:
                        result = nmLeft.notEqualTo(right);
                        break;
                    default:
                        throw new BugInCF("ValueTransfer: unsupported operation: " + op);
                }
                resultValues.add(result);
                if (result) {
                    thenLeftVals.add(left);
                    thenRightVals.add(right);
                } else {
                    elseLeftVals.add(left);
                    elseRightVals.add(right);
                }
            }
        }

        createAnnotationFromResultsAndAddToStore(thenStore, thenLeftVals, leftNode);
        createAnnotationFromResultsAndAddToStore(elseStore, elseLeftVals, leftNode);
        createAnnotationFromResultsAndAddToStore(thenStore, thenRightVals, rightNode);
        createAnnotationFromResultsAndAddToStore(elseStore, elseRightVals, rightNode);

        return resultValues;
    }

    /**
     * Calculates the result of a binary comparison on a pair of intRange annotations, and refines
     * annotations appropriately.
     */
    private List<Boolean> refineIntRanges(
            Node leftNode,
            AnnotationMirror leftAnno,
            Node rightNode,
            AnnotationMirror rightAnno,
            ComparisonOperators op,
            CFStore thenStore,
            CFStore elseStore) {

        Range leftRange = getIntRangeFromAnnotation(leftNode, leftAnno);
        Range rightRange = getIntRangeFromAnnotation(rightNode, rightAnno);

        final Range thenRightRange;
        final Range thenLeftRange;
        final Range elseRightRange;
        final Range elseLeftRange;

        switch (op) {
            case EQUAL:
                thenRightRange = rightRange.refineEqualTo(leftRange);
                thenLeftRange = thenRightRange; // Only needs to be computed once.
                elseRightRange = rightRange.refineNotEqualTo(leftRange);
                elseLeftRange = leftRange.refineNotEqualTo(rightRange);
                break;
            case GREATER_THAN:
                thenLeftRange = leftRange.refineGreaterThan(rightRange);
                thenRightRange = rightRange.refineLessThan(leftRange);
                elseRightRange = rightRange.refineGreaterThanEq(leftRange);
                elseLeftRange = leftRange.refineLessThanEq(rightRange);
                break;
            case GREATER_THAN_EQ:
                thenRightRange = rightRange.refineLessThanEq(leftRange);
                thenLeftRange = leftRange.refineGreaterThanEq(rightRange);
                elseLeftRange = leftRange.refineLessThan(rightRange);
                elseRightRange = rightRange.refineGreaterThan(leftRange);
                break;
            case LESS_THAN:
                thenLeftRange = leftRange.refineLessThan(rightRange);
                thenRightRange = rightRange.refineGreaterThan(leftRange);
                elseRightRange = rightRange.refineLessThanEq(leftRange);
                elseLeftRange = leftRange.refineGreaterThanEq(rightRange);
                break;
            case LESS_THAN_EQ:
                thenRightRange = rightRange.refineGreaterThanEq(leftRange);
                thenLeftRange = leftRange.refineLessThanEq(rightRange);
                elseLeftRange = leftRange.refineGreaterThan(rightRange);
                elseRightRange = rightRange.refineLessThan(leftRange);
                break;
            case NOT_EQUAL:
                thenRightRange = rightRange.refineNotEqualTo(leftRange);
                thenLeftRange = leftRange.refineNotEqualTo(rightRange);
                elseRightRange = rightRange.refineEqualTo(leftRange);
                elseLeftRange = elseRightRange; // Equality only needs to be computed once.
                break;
            default:
                throw new BugInCF("ValueTransfer: unsupported operation: " + op);
        }

        createAnnotationFromRangeAndAddToStore(thenStore, thenRightRange, rightNode);
        createAnnotationFromRangeAndAddToStore(thenStore, thenLeftRange, leftNode);
        createAnnotationFromRangeAndAddToStore(elseStore, elseRightRange, rightNode);
        createAnnotationFromRangeAndAddToStore(elseStore, elseLeftRange, leftNode);

        // TODO: Refine the type of the comparison.
        return null;
    }

    /**
     * Takes a list of result values (i.e. the values possible after the comparison) and creates the
     * appropriate annotation from them, then combines that annotation with the existing annotation
     * on the node. The resulting annotation is inserted into the store.
     */
    private void createAnnotationFromResultsAndAddToStore(
            CFStore store, List<?> results, Node node) {
        AnnotationMirror anno = atypefactory.createResultingAnnotation(node.getType(), results);
        addAnnotationToStore(store, anno, node);
    }

    /**
     * Takes a range and creates the appropriate annotation from it, then combines that annotation
     * with the existing annotation on the node. The resulting annotation is inserted into the
     * store.
     */
    private void createAnnotationFromRangeAndAddToStore(CFStore store, Range range, Node node) {
        AnnotationMirror anno = atypefactory.createIntRangeAnnotation(range);
        addAnnotationToStore(store, anno, node);
    }

    private void addAnnotationToStore(CFStore store, AnnotationMirror anno, Node node) {
        // If node is assignment, iterate over lhs and rhs; otherwise, iterator contains just node.
        for (Node internal : splitAssignments(node)) {
            Receiver rec = FlowExpressions.internalReprOf(analysis.getTypeFactory(), internal);
            CFValue currentValueFromStore;
            if (CFAbstractStore.canInsertReceiver(rec)) {
                currentValueFromStore = store.getValue(rec);
            } else {
                // Don't just `continue;` which would skip the calls to refine{Array,String}...
                currentValueFromStore = null;
            }
            AnnotationMirror currentAnno =
                    (currentValueFromStore == null
                            ? atypefactory.UNKNOWNVAL
                            : getValueAnnotation(currentValueFromStore));
            // Combine the new annotations based on the results of the comparison with the existing
            // type.
            AnnotationMirror newAnno = hierarchy.greatestLowerBound(anno, currentAnno);
            store.insertValue(rec, newAnno);

            if (node instanceof FieldAccessNode) {
                refineArrayAtLengthAccess((FieldAccessNode) internal, store);
            } else if (node instanceof MethodInvocationNode) {
                refineStringAtLengthInvocation((MethodInvocationNode) internal, store);
            }
        }
    }

    @Override
    public TransferResult<CFValue, CFStore> visitLessThan(
            LessThanNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<CFValue, CFStore> transferResult = super.visitLessThan(n, p);
        CFStore thenStore = transferResult.getThenStore();
        CFStore elseStore = transferResult.getElseStore();
        List<Boolean> resultValues =
                calculateBinaryComparison(
                        n.getLeftOperand(),
                        p.getValueOfSubNode(n.getLeftOperand()),
                        n.getRightOperand(),
                        p.getValueOfSubNode(n.getRightOperand()),
                        ComparisonOperators.LESS_THAN,
                        thenStore,
                        elseStore);
        TypeMirror underlyingType = transferResult.getResultValue().getUnderlyingType();
        return createNewResultBoolean(thenStore, elseStore, resultValues, underlyingType);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitLessThanOrEqual(
            LessThanOrEqualNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<CFValue, CFStore> transferResult = super.visitLessThanOrEqual(n, p);
        CFStore thenStore = transferResult.getThenStore();
        CFStore elseStore = transferResult.getElseStore();
        List<Boolean> resultValues =
                calculateBinaryComparison(
                        n.getLeftOperand(),
                        p.getValueOfSubNode(n.getLeftOperand()),
                        n.getRightOperand(),
                        p.getValueOfSubNode(n.getRightOperand()),
                        ComparisonOperators.LESS_THAN_EQ,
                        thenStore,
                        elseStore);
        TypeMirror underlyingType = transferResult.getResultValue().getUnderlyingType();
        return createNewResultBoolean(thenStore, elseStore, resultValues, underlyingType);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitGreaterThan(
            GreaterThanNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<CFValue, CFStore> transferResult = super.visitGreaterThan(n, p);
        CFStore thenStore = transferResult.getThenStore();
        CFStore elseStore = transferResult.getElseStore();
        List<Boolean> resultValues =
                calculateBinaryComparison(
                        n.getLeftOperand(),
                        p.getValueOfSubNode(n.getLeftOperand()),
                        n.getRightOperand(),
                        p.getValueOfSubNode(n.getRightOperand()),
                        ComparisonOperators.GREATER_THAN,
                        thenStore,
                        elseStore);
        TypeMirror underlyingType = transferResult.getResultValue().getUnderlyingType();
        return createNewResultBoolean(thenStore, elseStore, resultValues, underlyingType);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitGreaterThanOrEqual(
            GreaterThanOrEqualNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<CFValue, CFStore> transferResult = super.visitGreaterThanOrEqual(n, p);
        CFStore thenStore = transferResult.getThenStore();
        CFStore elseStore = transferResult.getElseStore();
        List<Boolean> resultValues =
                calculateBinaryComparison(
                        n.getLeftOperand(),
                        p.getValueOfSubNode(n.getLeftOperand()),
                        n.getRightOperand(),
                        p.getValueOfSubNode(n.getRightOperand()),
                        ComparisonOperators.GREATER_THAN_EQ,
                        thenStore,
                        elseStore);
        TypeMirror underlyingType = transferResult.getResultValue().getUnderlyingType();
        return createNewResultBoolean(thenStore, elseStore, resultValues, underlyingType);
    }

    @Override
    protected TransferResult<CFValue, CFStore> strengthenAnnotationOfEqualTo(
            TransferResult<CFValue, CFStore> transferResult,
            Node firstNode,
            Node secondNode,
            CFValue firstValue,
            CFValue secondValue,
            boolean notEqualTo) {
        if (firstValue == null) {
            return transferResult;
        }
        if (TypesUtils.isNumeric(firstNode.getType())
                || TypesUtils.isNumeric(secondNode.getType())) {
            CFStore thenStore = transferResult.getThenStore();
            CFStore elseStore = transferResult.getElseStore();
            // At least one must be a primitive otherwise reference equality is used.
            List<Boolean> resultValues =
                    calculateBinaryComparison(
                            firstNode,
                            firstValue,
                            secondNode,
                            secondValue,
                            notEqualTo ? ComparisonOperators.NOT_EQUAL : ComparisonOperators.EQUAL,
                            thenStore,
                            elseStore);
            if (transferResult.getResultValue() == null) {
                // Happens for case labels
                return transferResult;
            }
            TypeMirror underlyingType = transferResult.getResultValue().getUnderlyingType();
            return createNewResultBoolean(thenStore, elseStore, resultValues, underlyingType);
        }
        return super.strengthenAnnotationOfEqualTo(
                transferResult, firstNode, secondNode, firstValue, secondValue, notEqualTo);
    }

    @Override
    protected void processConditionalPostconditions(
            MethodInvocationNode n,
            ExecutableElement methodElement,
            Tree tree,
            CFStore thenStore,
            CFStore elseStore) {
        // For String.startsWith(String) and String.endsWith(String), refine the minimum length
        // of the receiver to the minimum length of the argument.

        ValueMethodIdentifier methodIdentifier = atypefactory.getMethodIdentifier();
        if (methodIdentifier.isStartsWithMethod(methodElement)
                || methodIdentifier.isEndsWithMethod(methodElement)) {

            Node argumentNode = n.getArgument(0);
            AnnotationMirror argumentAnno = getArrayOrStringAnnotation(argumentNode);
            int minLength = atypefactory.getMinLenValue(argumentAnno);
            // Update the annotation of the receiver
            if (minLength != 0) {
                Receiver receiver =
                        FlowExpressions.internalReprOf(atypefactory, n.getTarget().getReceiver());

                AnnotationMirror minLenAnno =
                        atypefactory.createArrayLenRangeAnnotation(minLength, Integer.MAX_VALUE);
                thenStore.insertValue(receiver, minLenAnno);
            }
        }

        super.processConditionalPostconditions(n, methodElement, tree, thenStore, elseStore);
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
        throw new BugInCF("ValueTransfer: unsupported operation: " + op);
    }

    @Override
    public TransferResult<CFValue, CFStore> visitConditionalNot(
            ConditionalNotNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<CFValue, CFStore> transferResult = super.visitConditionalNot(n, p);
        List<Boolean> resultValues =
                calculateConditionalOperator(n.getOperand(), null, ConditionalOperators.NOT, p);
        return createNewResultBoolean(
                transferResult.getThenStore(),
                transferResult.getElseStore(),
                resultValues,
                transferResult.getResultValue().getUnderlyingType());
    }

    @Override
    public TransferResult<CFValue, CFStore> visitConditionalAnd(
            ConditionalAndNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<CFValue, CFStore> transferResult = super.visitConditionalAnd(n, p);
        List<Boolean> resultValues =
                calculateConditionalOperator(
                        n.getLeftOperand(), n.getRightOperand(), ConditionalOperators.AND, p);
        return createNewResultBoolean(
                transferResult.getThenStore(),
                transferResult.getElseStore(),
                resultValues,
                transferResult.getResultValue().getUnderlyingType());
    }

    @Override
    public TransferResult<CFValue, CFStore> visitConditionalOr(
            ConditionalOrNode n, TransferInput<CFValue, CFStore> p) {
        TransferResult<CFValue, CFStore> transferResult = super.visitConditionalOr(n, p);
        List<Boolean> resultValues =
                calculateConditionalOperator(
                        n.getLeftOperand(), n.getRightOperand(), ConditionalOperators.OR, p);
        return createNewResultBoolean(
                transferResult.getThenStore(),
                transferResult.getElseStore(),
                resultValues,
                transferResult.getResultValue().getUnderlyingType());
    }
}
