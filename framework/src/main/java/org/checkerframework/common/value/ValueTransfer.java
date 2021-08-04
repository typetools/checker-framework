package org.checkerframework.common.value;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.common.value.qual.ArrayLen;
import org.checkerframework.common.value.qual.ArrayLenRange;
import org.checkerframework.common.value.qual.StringVal;
import org.checkerframework.common.value.util.NumberMath;
import org.checkerframework.common.value.util.NumberUtils;
import org.checkerframework.common.value.util.Range;
import org.checkerframework.dataflow.analysis.ConditionalTransferResult;
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
import org.checkerframework.dataflow.cfg.node.StringLiteralNode;
import org.checkerframework.dataflow.cfg.node.UnsignedRightShiftNode;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.dataflow.expression.Unknown;
import org.checkerframework.dataflow.util.NodeUtils;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFAbstractStore;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFTransfer;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypeSystemError;
import org.checkerframework.javacutil.TypesUtils;
import org.plumelib.util.CollectionsPlume;

/** The transfer class for the Value Checker. */
public class ValueTransfer extends CFTransfer {
  /** The Value type factory. */
  protected final ValueAnnotatedTypeFactory atypeFactory;
  /** The Value qualifier hierarchy. */
  protected final QualifierHierarchy hierarchy;

  /**
   * Create a new ValueTransfer.
   *
   * @param analysis the corresponding analysis
   */
  public ValueTransfer(CFAbstractAnalysis<CFValue, CFStore, CFTransfer> analysis) {
    super(analysis);
    atypeFactory = (ValueAnnotatedTypeFactory) analysis.getTypeFactory();
    hierarchy = atypeFactory.getQualifierHierarchy();
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

    return Range.create(lowerLength, upperLength);
  }

  /**
   * Returns a range of possible lengths for {@code subNode}, as casted to a String.
   *
   * @param subNode some subnode of {@code p}
   * @param p TransferInput
   * @return a range of possible lengths for {@code subNode}, as casted to a String
   */
  private Range getStringLengthRange(Node subNode, TransferInput<CFValue, CFStore> p) {
    CFValue value = p.getValueOfSubNode(subNode);

    AnnotationMirror anno = getValueAnnotation(value);
    if (anno == null) {
      return null;
    }
    String annoName = AnnotationUtils.annotationName(anno);
    if (annoName.equals(ValueAnnotatedTypeFactory.ARRAYLENRANGE_NAME)) {
      return atypeFactory.getRange(anno);
    } else if (annoName.equals(ValueAnnotatedTypeFactory.BOTTOMVAL_NAME)) {
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
      return Range.create(1, 11);
    } else if (subNodeTypeKind == TypeKind.LONG) {
      // longs are between 1 and 20 characters long
      return Range.create(1, 20);
    }

    return Range.create(0, Integer.MAX_VALUE);
  }

  /**
   * Returns a list of possible lengths for {@code subNode}, as casted to a String. Returns null if
   * {@code subNode}'s type is top/unknown. Returns an empty list if {@code subNode}'s type is
   * bottom.
   */
  private List<Integer> getStringLengths(Node subNode, TransferInput<CFValue, CFStore> p) {

    CFValue value = p.getValueOfSubNode(subNode);
    AnnotationMirror anno = getValueAnnotation(value);
    if (anno == null) {
      return null;
    }
    String annoName = AnnotationUtils.annotationName(anno);
    if (annoName.equals(ValueAnnotatedTypeFactory.ARRAYLEN_NAME)) {
      return atypeFactory.getArrayLength(anno);
    } else if (annoName.equals(ValueAnnotatedTypeFactory.BOTTOMVAL_NAME)) {
      return Collections.emptyList();
    }

    TypeKind subNodeTypeKind = subNode.getType().getKind();

    // handle values converted to string (characters, bytes, shorts, ints with @IntRange)
    if (subNode instanceof StringConversionNode) {
      return getStringLengths(((StringConversionNode) subNode).getOperand(), p);
    } else if (subNodeTypeKind == TypeKind.CHAR) {
      // characters always have length 1
      return Collections.singletonList(1);
    } else if (isIntRange(subNode, p)) {
      // Try to get a list of lengths from a range of integer values converted to string @IntVal is
      // not checked for, because if it is present, we would already have the actual string values
      Range lengthRange = getIntRangeStringLengthRange(subNode, p);
      return ValueCheckerUtils.getValuesFromRange(lengthRange, Integer.class);
    } else if (subNodeTypeKind == TypeKind.BYTE) {
      // bytes are between 1 and 4 characters long
      return ValueCheckerUtils.getValuesFromRange(Range.create(1, 4), Integer.class);
    } else if (subNodeTypeKind == TypeKind.SHORT) {
      // shorts are between 1 and 6 characters long
      return ValueCheckerUtils.getValuesFromRange(Range.create(1, 6), Integer.class);
    } else {
      return null;
    }
  }

  /**
   * Returns a list of possible values for {@code subNode}, as casted to a String. Returns null if
   * {@code subNode}'s type is top/unknown. Returns an empty list if {@code subNode}'s type is
   * bottom.
   *
   * @param subNode a subNode of p
   * @param p TransferInput
   * @return a list of possible values for {@code subNode} or null
   */
  private List<String> getStringValues(Node subNode, TransferInput<CFValue, CFStore> p) {
    CFValue value = p.getValueOfSubNode(subNode);
    AnnotationMirror anno = getValueAnnotation(value);
    if (anno == null) {
      return null;
    }
    String annoName = AnnotationUtils.annotationName(anno);
    switch (annoName) {
      case ValueAnnotatedTypeFactory.UNKNOWN_NAME:
        return null;
      case ValueAnnotatedTypeFactory.BOTTOMVAL_NAME:
        return Collections.emptyList();
      case ValueAnnotatedTypeFactory.STRINGVAL_NAME:
        return atypeFactory.getStringValues(anno);
      default:
        // Do nothing.
    }

    // @IntVal, @IntRange, @DoubleVal, @BoolVal (have to be converted to string)
    List<? extends Object> values;
    if (annoName.equals(ValueAnnotatedTypeFactory.BOOLVAL_NAME)) {
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
    List<String> stringValues = CollectionsPlume.mapList(Object::toString, values);
    // Empty list means bottom value
    return stringValues.isEmpty() ? Collections.singletonList("null") : stringValues;
  }

  /**
   * Create a @BoolVal CFValue for the given boolean value.
   *
   * @param value the value for the @BoolVal annotation
   * @return a @BoolVal CFValue for the given boolean value
   */
  private CFValue createBooleanCFValue(boolean value) {
    return analysis.createSingleAnnotationValue(
        value ? atypeFactory.BOOLEAN_TRUE : atypeFactory.BOOLEAN_FALSE,
        atypeFactory.types.getPrimitiveType(TypeKind.BOOLEAN));
  }

  /**
   * Get the unique possible boolean value from @BoolVal. Returns null if that is not the case
   * (including if the CFValue is not @BoolVal).
   *
   * @param value a CFValue
   * @return theboolean if {@code value} represents a single boolean value; otherwise null
   */
  private Boolean getBooleanValue(CFValue value) {
    AnnotationMirror boolAnno =
        AnnotationUtils.getAnnotationByName(
            value.getAnnotations(), ValueAnnotatedTypeFactory.BOOLVAL_NAME);
    return atypeFactory.getBooleanValue(boolAnno);
  }

  /**
   * Get possible boolean values for a node. Returns null if there is no estimate, because the
   * node's value is not @BoolVal.
   *
   * @param subNode the node whose value to obtain
   * @param p the transfer input in which to look up values
   * @return the possible boolean values for the node
   */
  private List<Boolean> getBooleanValues(Node subNode, TransferInput<CFValue, CFStore> p) {
    CFValue value = p.getValueOfSubNode(subNode);
    AnnotationMirror intAnno =
        AnnotationUtils.getAnnotationByName(
            value.getAnnotations(), ValueAnnotatedTypeFactory.BOOLVAL_NAME);
    return atypeFactory.getBooleanValues(intAnno);
  }

  /** Get possible char values from annotation @IntRange or @IntVal. */
  private List<Character> getCharValues(Node subNode, TransferInput<CFValue, CFStore> p) {
    CFValue value = p.getValueOfSubNode(subNode);
    AnnotationMirror intAnno;

    intAnno =
        AnnotationUtils.getAnnotationByName(
            value.getAnnotations(), ValueAnnotatedTypeFactory.INTVAL_NAME);
    if (intAnno != null) {
      return atypeFactory.getCharValues(intAnno);
    }

    if (atypeFactory.isIntRange(value.getAnnotations())) {
      intAnno =
          hierarchy.findAnnotationInHierarchy(value.getAnnotations(), atypeFactory.UNKNOWNVAL);
      Range range = atypeFactory.getRange(intAnno);
      return ValueCheckerUtils.getValuesFromRange(range, Character.class);
    }

    return Collections.emptyList();
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
    return hierarchy.findAnnotationInHierarchy(cfValue.getAnnotations(), atypeFactory.UNKNOWNVAL);
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

  /**
   * Returns the numerical values in valueAnno casted to the type of subNode.
   *
   * @param subNode node
   * @param valueAnno annotation mirror
   * @return the numerical values in valueAnno casted to the type of subNode
   */
  private List<? extends Number> getNumericalValues(Node subNode, AnnotationMirror valueAnno) {

    if (valueAnno == null
        || AnnotationUtils.areSameByName(valueAnno, ValueAnnotatedTypeFactory.UNKNOWN_NAME)) {
      return null;
    } else if (AnnotationUtils.areSameByName(valueAnno, ValueAnnotatedTypeFactory.BOTTOMVAL_NAME)) {
      return Collections.emptyList();
    }
    List<? extends Number> values;
    if (AnnotationUtils.areSameByName(valueAnno, ValueAnnotatedTypeFactory.INTVAL_NAME)) {
      values = atypeFactory.getIntValues(valueAnno);
    } else if (AnnotationUtils.areSameByName(valueAnno, ValueAnnotatedTypeFactory.DOUBLEVAL_NAME)) {
      values = atypeFactory.getDoubleValues(valueAnno);
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

  /**
   * Returns the {@link Range} object corresponding to the annotation {@code val} casted to the type
   * of {@code node}.
   *
   * @param node a node
   * @param val annotation mirror
   * @return the {@link Range} object corresponding to the annotation {@code val} casted to the type
   *     of {@code node}.
   */
  private Range getIntRangeFromAnnotation(Node node, AnnotationMirror val) {
    Range range;
    if (val == null || AnnotationUtils.areSameByName(val, ValueAnnotatedTypeFactory.UNKNOWN_NAME)) {
      range = Range.EVERYTHING;
    } else if (atypeFactory.isIntRange(val)) {
      range = atypeFactory.getRange(val);
    } else if (AnnotationUtils.areSameByName(val, ValueAnnotatedTypeFactory.INTVAL_NAME)) {
      List<Long> values = atypeFactory.getIntValues(val);
      range = ValueCheckerUtils.getRangeFromValues(values);
    } else if (AnnotationUtils.areSameByName(val, ValueAnnotatedTypeFactory.DOUBLEVAL_NAME)) {
      List<Double> values = atypeFactory.getDoubleValues(val);
      range = ValueCheckerUtils.getRangeFromValues(values);
    } else if (AnnotationUtils.areSameByName(val, ValueAnnotatedTypeFactory.BOTTOMVAL_NAME)) {
      return Range.NOTHING;
    } else {
      range = Range.EVERYTHING;
    }
    return NumberUtils.castRange(node.getType(), range);
  }

  /**
   * Returns true if subNode is annotated with {@code @IntRange}.
   *
   * @param subNode subNode of {@code p}
   * @param p TransferInput
   * @return true if this subNode is annotated with {@code @IntRange}
   */
  private boolean isIntRange(Node subNode, TransferInput<CFValue, CFStore> p) {
    CFValue value = p.getValueOfSubNode(subNode);
    return atypeFactory.isIntRange(value.getAnnotations());
  }

  /**
   * Returns true if {@code node} an integral type and is {@code anno} is {@code @UnknownVal}.
   *
   * @param node a node
   * @param anno annotation mirror
   * @return true if node is annotated with {@code @UnknownVal} and it is an integral type
   */
  private boolean isIntegralUnknownVal(Node node, AnnotationMirror anno) {
    return AnnotationUtils.areSameByName(anno, ValueAnnotatedTypeFactory.UNKNOWN_NAME)
        && TypesUtils.isIntegralPrimitive(node.getType());
  }

  /**
   * Returns true if this node is annotated with {@code @IntRange} or {@code @UnknownVal}.
   *
   * @param node the node to inspect
   * @param p storage
   * @return true if this node is annotated with {@code @IntRange} or {@code @UnknownVal}
   */
  private boolean isIntRangeOrIntegralUnknownVal(Node node, TransferInput<CFValue, CFStore> p) {
    if (isIntRange(node, p)) {
      return true;
    }
    return isIntegralUnknownVal(node, getValueAnnotation(p.getValueOfSubNode(node)));
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
      CFStore thenStore, CFStore elseStore, List<Boolean> resultValues, TypeMirror underlyingType) {
    AnnotationMirror boolVal = atypeFactory.createBooleanAnnotation(resultValues);
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
    refineAtLengthInvocation(n, result.getRegularStore());
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
   * If length method is invoked for a sequence, transform its @IntVal annotation into an @ArrayLen
   * annotation.
   *
   * @param lengthNode the length method invocation node
   * @param store the Checker Framework store
   */
  private void refineAtLengthInvocation(MethodInvocationNode lengthNode, CFStore store) {
    if (atypeFactory
        .getMethodIdentifier()
        .isStringLengthMethod(lengthNode.getTarget().getMethod())) {
      MethodAccessNode methodAccessNode = lengthNode.getTarget();
      refineAtLengthAccess(lengthNode, methodAccessNode.getReceiver(), store);
    } else if (atypeFactory
        .getMethodIdentifier()
        .isArrayGetLengthMethod(lengthNode.getTarget().getMethod())) {
      Node node = lengthNode.getArguments().get(0);
      refineAtLengthAccess(lengthNode, node, store);
    }
  }

  /**
   * Gets a value checker annotation relevant for an array or a string.
   *
   * @param arrayOrStringNode the node whose annotation to return
   * @return the value checker annotation for the array or a string
   */
  private AnnotationMirror getArrayOrStringAnnotation(Node arrayOrStringNode) {
    AnnotationMirror arrayOrStringAnno =
        atypeFactory.getAnnotationMirror(arrayOrStringNode.getTree(), StringVal.class);
    if (arrayOrStringAnno == null) {
      arrayOrStringAnno =
          atypeFactory.getAnnotationMirror(arrayOrStringNode.getTree(), ArrayLen.class);
    }
    if (arrayOrStringAnno == null) {
      arrayOrStringAnno =
          atypeFactory.getAnnotationMirror(arrayOrStringNode.getTree(), ArrayLenRange.class);
    }

    return arrayOrStringAnno;
  }

  /**
   * Transform @IntVal or @IntRange annotations of a array or string length into an @ArrayLen
   * or @ArrayLenRange annotation for the array or string.
   *
   * @param lengthNode an invocation of method {@code length} or an access of the {@code length}
   *     field
   * @param receiverNode the receiver of {@code lengthNode}
   * @param store the store to update
   */
  private void refineAtLengthAccess(Node lengthNode, Node receiverNode, CFStore store) {
    JavaExpression lengthExpr = JavaExpression.fromNode(lengthNode);

    // If the expression is not representable (for example if String.length() for some reason is
    // not marked @Pure, then do not refine.
    if (lengthExpr instanceof Unknown) {
      return;
    }

    CFValue value = store.getValue(lengthExpr);
    if (value == null) {
      return;
    }

    AnnotationMirror lengthAnno = getValueAnnotation(value);
    if (lengthAnno == null) {
      return;
    }
    if (AnnotationUtils.areSameByName(lengthAnno, ValueAnnotatedTypeFactory.BOTTOMVAL_NAME)) {
      // If the length is bottom, then this is dead code, so the receiver type
      // should also be bottom.
      JavaExpression receiver = JavaExpression.fromNode(receiverNode);
      store.insertValue(receiver, lengthAnno);
      return;
    }

    RangeOrListOfValues rolv;
    if (atypeFactory.isIntRange(lengthAnno)) {
      rolv = new RangeOrListOfValues(atypeFactory.getRange(lengthAnno));
    } else if (AnnotationUtils.areSameByName(lengthAnno, ValueAnnotatedTypeFactory.INTVAL_NAME)) {
      List<Long> lengthValues = atypeFactory.getIntValues(lengthAnno);
      rolv = new RangeOrListOfValues(RangeOrListOfValues.convertLongsToInts(lengthValues));
    } else {
      return;
    }
    AnnotationMirror newRecAnno = rolv.createAnnotation(atypeFactory);
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
    JavaExpression receiver = JavaExpression.fromNode(receiverNode);
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
   * Calculates possible lengths of a result of string concatenation of strings with known lengths.
   */
  private List<Integer> calculateLengthAddition(
      List<Integer> leftLengths, List<Integer> rightLengths) {
    List<Integer> result = new ArrayList<>(leftLengths.size() * rightLengths.size());

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

  /**
   * Checks whether or not the passed node is nullable. This superficial check assumes that every
   * node is nullable unless it is a primitive, String literal, or compile-time constant.
   *
   * @return false if the node's run-time can't be null; true if the node's run-time value may be
   *     null, or if this method is not precise enough
   */
  private boolean isNullable(Node node) {
    if (node instanceof StringConversionNode) {
      if (((StringConversionNode) node).getOperand().getType().getKind().isPrimitive()) {
        return false;
      }
    } else if (node instanceof StringLiteralNode) {
      return false;
    } else if (node instanceof StringConcatenateNode) {
      return false;
    }

    Element element = TreeUtils.elementFromUse((ExpressionTree) node.getTree());
    return !ElementUtils.isCompileTimeConstant(element);
  }

  /** Creates an annotation for a result of string concatenation. */
  private AnnotationMirror createAnnotationForStringConcatenation(
      Node leftOperand, Node rightOperand, TransferInput<CFValue, CFStore> p) {
    boolean nonNullStringConcat =
        atypeFactory.getChecker().hasOption("nonNullStringsConcatenation");

    // Try using sets of string values
    List<String> leftValues = getStringValues(leftOperand, p);
    List<String> rightValues = getStringValues(rightOperand, p);
    if (leftValues != null && rightValues != null) {
      // Both operands have known string values, compute set of results
      leftValues = appendNullToStrings(nonNullStringConcat, leftOperand, leftValues);
      rightValues = appendNullToStrings(nonNullStringConcat, rightOperand, rightValues);
      List<String> concatValues = new ArrayList<>(leftValues.size() * rightValues.size());
      for (String left : leftValues) {
        for (String right : rightValues) {
          concatValues.add(left + right);
        }
      }
      return atypeFactory.createStringAnnotation(concatValues);
    }

    // Try using sets of lengths
    List<Integer> leftLengths = getStringLengths(leftOperand, p, leftValues);
    List<Integer> rightLengths = getStringLengths(rightOperand, p, rightValues);
    if (leftLengths != null && rightLengths != null) {
      // Both operands have known lengths, compute set of result lengths
      leftLengths = appendNullToIntegers(nonNullStringConcat, leftOperand, leftLengths);
      rightLengths = appendNullToIntegers(nonNullStringConcat, rightOperand, rightLengths);
      List<Integer> concatLengths = calculateLengthAddition(leftLengths, rightLengths);
      return atypeFactory.createArrayLenAnnotation(concatLengths);
    }

    // Try using ranges of lengths
    Range leftLengthRange = getStringLengthRange(leftOperand, p, leftLengths);
    Range rightLengthRange = getStringLengthRange(rightOperand, p, rightLengths);
    if (leftLengthRange != null && rightLengthRange != null) {
      // Both operands have a length from a known range, compute a range of result lengths
      leftLengthRange = appendNullToLengthRange(nonNullStringConcat, leftOperand, leftLengthRange);
      rightLengthRange =
          appendNullToLengthRange(nonNullStringConcat, rightOperand, rightLengthRange);
      Range concatLengthRange = calculateLengthRangeAddition(leftLengthRange, rightLengthRange);
      return atypeFactory.createArrayLenRangeAnnotation(concatLengthRange);
    }

    return atypeFactory.UNKNOWNVAL;
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
      Node leftNode, Node rightNode, NumericalBinaryOps op, TransferInput<CFValue, CFStore> p) {
    if (!isIntRangeOrIntegralUnknownVal(leftNode, p)
        && !isIntRangeOrIntegralUnknownVal(rightNode, p)) {
      List<Number> resultValues = calculateValuesBinaryOp(leftNode, rightNode, op, p);
      return atypeFactory.createNumberAnnotationMirror(resultValues);
    } else {
      Range resultRange = calculateRangeBinaryOp(leftNode, rightNode, op, p);
      return atypeFactory.createIntRangeAnnotation(resultRange);
    }
  }

  /** Calculate the result range after a binary operation between two numerical type nodes. */
  private Range calculateRangeBinaryOp(
      Node leftNode, Node rightNode, NumericalBinaryOps op, TransferInput<CFValue, CFStore> p) {
    if (TypesUtils.isIntegralPrimitive(leftNode.getType())
        && TypesUtils.isIntegralPrimitive(rightNode.getType())) {
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
          throw new TypeSystemError("ValueTransfer: unsupported operation: " + op);
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
      Node leftNode, Node rightNode, NumericalBinaryOps op, TransferInput<CFValue, CFStore> p) {
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
            throw new TypeSystemError("ValueTransfer: unsupported operation: " + op);
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
            n.getLeftOperand(), n.getRightOperand(), NumericalBinaryOps.MULTIPLICATION, p);
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
            n.getLeftOperand(), n.getRightOperand(), NumericalBinaryOps.SIGNED_SHIFT_RIGHT, p);
    return createNewResult(transferResult, resultAnno);
  }

  @Override
  public TransferResult<CFValue, CFStore> visitUnsignedRightShift(
      UnsignedRightShiftNode n, TransferInput<CFValue, CFStore> p) {
    TransferResult<CFValue, CFStore> transferResult = super.visitUnsignedRightShift(n, p);
    AnnotationMirror resultAnno =
        calculateNumericalBinaryOp(
            n.getLeftOperand(), n.getRightOperand(), NumericalBinaryOps.UNSIGNED_SHIFT_RIGHT, p);
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
      return atypeFactory.createNumberAnnotationMirror(resultValues);
    } else {
      Range resultRange = calculateRangeUnaryOp(operand, op, p);
      return atypeFactory.createIntRangeAnnotation(resultRange);
    }
  }

  /**
   * Calculate the result range after a unary operation of a numerical type node.
   *
   * @param operand the node that represents the operand
   * @param op the operator type
   * @param p the transfer input
   * @return the result annotation mirror
   */
  private Range calculateRangeUnaryOp(
      Node operand, NumericalUnaryOps op, TransferInput<CFValue, CFStore> p) {
    if (TypesUtils.isIntegralPrimitive(operand.getType())) {
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
          throw new TypeSystemError("ValueTransfer: unsupported operation: " + op);
      }
      // Any integral type with less than 32 bits would be promoted to 32-bit int type during
      // operations.
      return operand.getType().getKind() == TypeKind.LONG ? resultRange : resultRange.intRange();
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
    List<Number> resultValues = new ArrayList<>(lefts.size());
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
          throw new TypeSystemError("ValueTransfer: unsupported operation: " + op);
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

    if (atypeFactory.isIntRange(leftAnno)
        || atypeFactory.isIntRange(rightAnno)
        || isIntegralUnknownVal(rightNode, rightAnno)
        || isIntegralUnknownVal(leftNode, leftAnno)) {
      // If either is @UnknownVal, then refineIntRanges will treat it as the max range and thus
      // refine it if possible.  Also, if either is an @IntVal, then it will be converted to a
      // range.  This is less precise in some cases, but avoids the complexity of comparing a list
      // of values to a range. (This could be implemented in the future.)
      return refineIntRanges(leftNode, leftAnno, rightNode, rightAnno, op, thenStore, elseStore);
    }

    List<? extends Number> lefts = getNumericalValues(leftNode, leftAnno);
    List<? extends Number> rights = getNumericalValues(rightNode, rightAnno);

    if (lefts == null || rights == null) {
      // Appropriately handle bottom when something is compared to bottom.
      if (AnnotationUtils.areSame(leftAnno, atypeFactory.BOTTOMVAL)
          || AnnotationUtils.areSame(rightAnno, atypeFactory.BOTTOMVAL)) {
        return Collections.emptyList();
      }
      return null;
    }

    // This is a list of all the values that the expression can evaluate to.
    List<Boolean> resultValues = new ArrayList<>();

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
            throw new TypeSystemError("ValueTransfer: unsupported operation: " + op);
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
        throw new TypeSystemError("ValueTransfer: unsupported operation: " + op);
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
   * appropriate annotation from them, then combines that annotation with the existing annotation on
   * the node. The resulting annotation is inserted into the store.
   *
   * @param store the store
   * @param results the result values
   * @param node the node whose existing annotation to refine
   */
  private void createAnnotationFromResultsAndAddToStore(CFStore store, List<?> results, Node node) {
    AnnotationMirror anno = atypeFactory.createResultingAnnotation(node.getType(), results);
    addAnnotationToStore(store, anno, node);
  }

  /**
   * Takes a range and creates the appropriate annotation from it, then combines that annotation
   * with the existing annotation on the node. The resulting annotation is inserted into the store.
   *
   * @param store the store
   * @param range the range to create an annotation for
   * @param node the node whose existing annotation to refine
   */
  private void createAnnotationFromRangeAndAddToStore(CFStore store, Range range, Node node) {
    AnnotationMirror anno = atypeFactory.createIntRangeAnnotation(range);
    addAnnotationToStore(store, anno, node);
  }

  private void addAnnotationToStore(CFStore store, AnnotationMirror anno, Node node) {
    // If node is assignment, iterate over lhs and rhs; otherwise, iterator contains just node.
    for (Node internal : splitAssignments(node)) {
      JavaExpression je = JavaExpression.fromNode(internal);
      CFValue currentValueFromStore;
      if (CFAbstractStore.canInsertJavaExpression(je)) {
        currentValueFromStore = store.getValue(je);
      } else {
        // Don't just `continue;` which would skip the calls to refine{Array,String}...
        currentValueFromStore = null;
      }
      AnnotationMirror currentAnno =
          (currentValueFromStore == null
              ? atypeFactory.UNKNOWNVAL
              : getValueAnnotation(currentValueFromStore));
      // Combine the new annotations based on the results of the comparison with the existing type.
      AnnotationMirror newAnno = hierarchy.greatestLowerBound(anno, currentAnno);
      store.insertValue(je, newAnno);

      if (node instanceof FieldAccessNode) {
        refineArrayAtLengthAccess((FieldAccessNode) internal, store);
      } else if (node instanceof MethodInvocationNode) {
        MethodInvocationNode miNode = (MethodInvocationNode) node;
        refineAtLengthInvocation(miNode, store);
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
    if (TypesUtils.isNumeric(firstNode.getType()) || TypesUtils.isNumeric(secondNode.getType())) {
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

    ValueMethodIdentifier methodIdentifier = atypeFactory.getMethodIdentifier();
    if (methodIdentifier.isStartsWithMethod(methodElement)
        || methodIdentifier.isEndsWithMethod(methodElement)) {

      Node argumentNode = n.getArgument(0);
      AnnotationMirror argumentAnno = getArrayOrStringAnnotation(argumentNode);
      int minLength = atypeFactory.getMinLenValue(argumentAnno);
      // Update the annotation of the receiver
      if (minLength != 0) {
        JavaExpression receiver = JavaExpression.fromNode(n.getTarget().getReceiver());

        AnnotationMirror minLenAnno =
            atypeFactory.createArrayLenRangeAnnotation(minLength, Integer.MAX_VALUE);
        thenStore.insertValuePermitNondeterministic(receiver, minLenAnno);
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
      Node leftNode, Node rightNode, ConditionalOperators op, TransferInput<CFValue, CFStore> p) {
    List<Boolean> lefts = getBooleanValues(leftNode, p);
    if (lefts == null) {
      lefts = ALL_BOOLEANS;
    }
    List<Boolean> rights = null;
    if (rightNode != null) {
      rights = getBooleanValues(rightNode, p);
      if (rights == null) {
        rights = ALL_BOOLEANS;
      }
    }
    // This list can contain duplicates.  It is deduplicated later by createBooleanAnnotation.
    List<Boolean> resultValues = new ArrayList<>(2);
    switch (op) {
      case NOT:
        return CollectionsPlume.mapList((Boolean left) -> !left, lefts);
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
    throw new TypeSystemError("ValueTransfer: unsupported operation: " + op);
  }

  @Override
  public TransferResult<CFValue, CFStore> visitEqualTo(
      EqualToNode n, TransferInput<CFValue, CFStore> p) {
    TransferResult<CFValue, CFStore> res = super.visitEqualTo(n, p);

    Node leftN = n.getLeftOperand();
    Node rightN = n.getRightOperand();
    CFValue leftV = p.getValueOfSubNode(leftN);
    CFValue rightV = p.getValueOfSubNode(rightN);

    // if annotations differ, use the one that is more precise for both
    // sides (and add it to the store if possible)
    res = strengthenAnnotationOfEqualTo(res, leftN, rightN, leftV, rightV, false);
    res = strengthenAnnotationOfEqualTo(res, rightN, leftN, rightV, leftV, false);

    Boolean leftBoolean = getBooleanValue(leftV);
    if (leftBoolean != null) {
      CFValue notLeftV = createBooleanCFValue(!leftBoolean);
      res = strengthenAnnotationOfEqualTo(res, leftN, rightN, notLeftV, rightV, true);
      res = strengthenAnnotationOfEqualTo(res, rightN, leftN, rightV, notLeftV, true);
    }
    Boolean rightBoolean = getBooleanValue(rightV);
    if (rightBoolean != null) {
      CFValue notRightV = createBooleanCFValue(!rightBoolean);
      res = strengthenAnnotationOfEqualTo(res, leftN, rightN, leftV, notRightV, true);
      res = strengthenAnnotationOfEqualTo(res, rightN, leftN, notRightV, leftV, true);
    }

    return res;
  }

  @Override
  public TransferResult<CFValue, CFStore> visitNotEqual(
      NotEqualNode n, TransferInput<CFValue, CFStore> p) {
    TransferResult<CFValue, CFStore> res = super.visitNotEqual(n, p);

    Node leftN = n.getLeftOperand();
    Node rightN = n.getRightOperand();
    CFValue leftV = p.getValueOfSubNode(leftN);
    CFValue rightV = p.getValueOfSubNode(rightN);

    // if annotations differ, use the one that is more precise for both
    // sides (and add it to the store if possible)
    res = strengthenAnnotationOfEqualTo(res, leftN, rightN, leftV, rightV, true);
    res = strengthenAnnotationOfEqualTo(res, rightN, leftN, rightV, leftV, true);

    Boolean leftBoolean = getBooleanValue(leftV);
    if (leftBoolean != null) {
      CFValue notLeftV = createBooleanCFValue(!leftBoolean);
      res = strengthenAnnotationOfEqualTo(res, leftN, rightN, notLeftV, rightV, false);
      res = strengthenAnnotationOfEqualTo(res, rightN, leftN, rightV, notLeftV, false);
    }
    Boolean rightBoolean = getBooleanValue(rightV);
    if (rightBoolean != null) {
      CFValue notRightV = createBooleanCFValue(!rightBoolean);
      res = strengthenAnnotationOfEqualTo(res, leftN, rightN, leftV, notRightV, false);
      res = strengthenAnnotationOfEqualTo(res, rightN, leftN, notRightV, leftV, false);
    }

    return res;
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

  /** convenience function used by createAnnotationForStringConcatenation(). */
  private List<Integer> getStringLengths(
      Node rightOperand, TransferInput<CFValue, CFStore> p, List<String> rightValues) {
    return rightValues != null
        ? ValueCheckerUtils.getLengthsForStringValues(rightValues)
        : getStringLengths(rightOperand, p);
  }

  /** convenience function used by createAnnotationForStringConcatenation(). */
  private Range getStringLengthRange(
      Node rightOperand, TransferInput<CFValue, CFStore> p, List<Integer> rightLengths) {
    return rightLengths != null
        ? ValueCheckerUtils.getRangeFromValues(rightLengths)
        : getStringLengthRange(rightOperand, p);
  }

  /** convenience function used by createAnnotationForStringConcatenation(). */
  private Range appendNullToLengthRange(
      boolean nonNullStringConcat, Node rightOperand, Range rightLengthRange) {
    if (!nonNullStringConcat) {
      if (isNullable(rightOperand)) {
        return rightLengthRange.union(Range.create(4, 4)); // "null"
      }
    }
    return rightLengthRange;
  }

  /** convenience function used by createAnnotationForStringConcatenation(). */
  private List<Integer> appendNullToIntegers(
      boolean nonNullStringConcat, Node rightOperand, List<Integer> rightLengths) {
    if (!nonNullStringConcat) {
      if (isNullable(rightOperand)) {
        rightLengths = new ArrayList<>(rightLengths);
        rightLengths.add(4); // "null"
      }
    }
    return rightLengths;
  }

  /** convenience function used by createAnnotationForStringConcatenation(). */
  private List<String> appendNullToStrings(
      boolean nonNullStringConcat, Node rightOperand, List<String> rightValues) {
    boolean append = false;
    if (!nonNullStringConcat) {
      if (isNullable(rightOperand)) {
        append = true;
      }
    } else {
      if (rightOperand instanceof StringConversionNode) {
        if (((StringConversionNode) rightOperand).getOperand().getType().getKind()
            == TypeKind.NULL) {
          append = true;
        }
      }
    }
    if (append) return CollectionsPlume.append(rightValues, "null");
    return rightValues;
  }
}
