package org.checkerframework.common.value;

import com.google.common.collect.Comparators;
import com.sun.source.tree.Tree;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.common.value.qual.IntRange;
import org.checkerframework.common.value.qual.IntVal;
import org.checkerframework.common.value.qual.StringVal;
import org.checkerframework.common.value.util.NumberUtils;
import org.checkerframework.common.value.util.Range;
import org.checkerframework.dataflow.expression.ArrayAccess;
import org.checkerframework.dataflow.expression.ArrayCreation;
import org.checkerframework.dataflow.expression.BinaryOperation;
import org.checkerframework.dataflow.expression.ClassName;
import org.checkerframework.dataflow.expression.FieldAccess;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.dataflow.expression.LocalVariable;
import org.checkerframework.dataflow.expression.MethodCall;
import org.checkerframework.dataflow.expression.ThisReference;
import org.checkerframework.dataflow.expression.UnaryOperation;
import org.checkerframework.dataflow.expression.Unknown;
import org.checkerframework.dataflow.expression.ValueLiteral;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.TypesUtils;

/** Utility methods for the Value Checker. */
public class ValueCheckerUtils {

    /** Do not instantiate. */
    private ValueCheckerUtils() {
        throw new BugInCF("do not instantiate");
    }

    /**
     * Get a list of values of annotation, and then cast them to a given type.
     *
     * @param anno the annotation that contains values
     * @param castTo the type that is casted to
     * @param atypeFactory the type factory
     * @return a list of values after the casting
     */
    public static List<?> getValuesCastedToType(
            AnnotationMirror anno, TypeMirror castTo, ValueAnnotatedTypeFactory atypeFactory) {
        Class<?> castType = TypesUtils.getClassFromType(castTo);
        List<?> values;
        switch (AnnotationUtils.annotationName(anno)) {
            case ValueAnnotatedTypeFactory.DOUBLEVAL_NAME:
                values = convertDoubleVal(anno, castType, castTo);
                break;
            case ValueAnnotatedTypeFactory.INTVAL_NAME:
                List<Long> longs = ValueAnnotatedTypeFactory.getIntValues(anno);
                values = convertIntVal(longs, castType, castTo);
                break;
            case ValueAnnotatedTypeFactory.INTRANGE_NAME:
                Range range = atypeFactory.getRange(anno);
                List<Long> rangeValues = getValuesFromRange(range, Long.class);
                values = convertIntVal(rangeValues, castType, castTo);
                break;
            case ValueAnnotatedTypeFactory.STRINGVAL_NAME:
                values = convertStringVal(anno, castType);
                break;
            case ValueAnnotatedTypeFactory.BOOLVAL_NAME:
                values = convertBoolVal(anno, castType);
                break;
            case ValueAnnotatedTypeFactory.BOTTOMVAL_NAME:
            case ValueAnnotatedTypeFactory.ARRAYLEN_NAME:
                values = new ArrayList<>();
                break;
            default:
                values = null;
        }
        return values;
    }

    /** Get the minimum and maximum of a list and return a range bounded by them. */
    public static Range getRangeFromValues(List<? extends Number> values) {
        if (values == null) {
            return null;
        } else if (values.isEmpty()) {
            return Range.NOTHING;
        }
        return Range.create(values);
    }

    /**
     * Converts a long value to a boxed numeric type.
     *
     * @param value a long value
     * @param expectedType the boxed numeric type of the result
     * @return {@code value} converted to {@code expectedType} using standard conversion rules
     */
    private static <T> T convertLongToType(long value, Class<T> expectedType) {
        Object convertedValue;
        if (expectedType == Integer.class) {
            convertedValue = (int) value;
        } else if (expectedType == Short.class) {
            convertedValue = (short) value;
        } else if (expectedType == Byte.class) {
            convertedValue = (byte) value;
        } else if (expectedType == Long.class) {
            convertedValue = value;
        } else if (expectedType == Double.class) {
            convertedValue = (double) value;
        } else if (expectedType == Float.class) {
            convertedValue = (float) value;
        } else if (expectedType == Character.class) {
            convertedValue = (char) value;
        } else {
            throw new UnsupportedOperationException(
                    "ValueCheckerUtils: unexpected class: " + expectedType);
        }
        return expectedType.cast(convertedValue);
    }

    /**
     * Get all possible values from the given type and cast them into a boxed primitive type.
     *
     * <p>{@code expectedType} must be a boxed type, not a primitive type, because primitive types
     * cannot be stored in a list.
     *
     * @param range the given range
     * @param expectedType the expected type
     * @return a list of all the values in the range
     */
    public static <T> List<T> getValuesFromRange(Range range, Class<T> expectedType) {
        if (range == null || range.isWiderThan(ValueAnnotatedTypeFactory.MAX_VALUES)) {
            return null;
        }
        List<T> values = new ArrayList<>();
        if (range.isNothing()) {
            return values;
        }

        // The subtraction does not overflow, because the width has already been checked, so the
        // bound difference is less than ValueAnnotatedTypeFactory.MAX_VALUES.
        long boundDifference = range.to - range.from;

        // Each value is computed as a sum of the first value and an offset within the range,
        // to avoid having range.to as an upper bound of the loop. range.to can be Long.MAX_VALUE,
        // in which case a comparison value <= range.to would be always true.
        // boundDifference is always much smaller than Long.MAX_VALUE
        for (long offset = 0; offset <= boundDifference; offset++) {
            long value = range.from + offset;
            values.add(convertLongToType(value, expectedType));
        }
        return values;
    }

    private static List<?> convertToStringVal(List<?> origValues) {
        if (origValues == null) {
            return null;
        }
        List<String> strings = new ArrayList<>();
        for (Object value : origValues) {
            strings.add(value.toString());
        }
        return strings;
    }

    private static List<?> convertBoolVal(AnnotationMirror anno, Class<?> newClass) {
        List<Boolean> bools =
                AnnotationUtils.getElementValueArray(anno, "value", Boolean.class, true);

        if (newClass == String.class) {
            return convertToStringVal(bools);
        }
        return bools;
    }

    private static List<?> convertStringVal(AnnotationMirror anno, Class<?> newClass) {
        List<String> strings = ValueAnnotatedTypeFactory.getStringValues(anno);
        if (newClass == char[].class) {
            List<char[]> chars = new ArrayList<>();
            for (String s : strings) {
                chars.add(s.toCharArray());
            }
            return chars;
        }
        return strings;
    }

    private static List<?> convertIntVal(List<Long> longs, Class<?> newClass, TypeMirror newType) {
        if (longs == null) {
            return null;
        }
        if (newClass == String.class) {
            return convertToStringVal(longs);
        } else if (newClass == Character.class || newClass == char.class) {
            List<Character> chars = new ArrayList<>();
            for (Long l : longs) {
                chars.add((char) l.longValue());
            }
            return chars;
        } else if (newClass == Boolean.class) {
            throw new UnsupportedOperationException(
                    "ValueAnnotatedTypeFactory: can't convert int to boolean");
        }
        return NumberUtils.castNumbers(newType, longs);
    }

    private static List<?> convertDoubleVal(
            AnnotationMirror anno, Class<?> newClass, TypeMirror newType) {
        List<Double> doubles = ValueAnnotatedTypeFactory.getDoubleValues(anno);
        if (doubles == null) {
            return null;
        }
        if (newClass == String.class) {
            return convertToStringVal(doubles);
        } else if (newClass == Character.class || newClass == char.class) {
            List<Character> chars = new ArrayList<>();
            for (Double l : doubles) {
                chars.add((char) l.doubleValue());
            }
            return chars;
        } else if (newClass == Boolean.class) {
            throw new UnsupportedOperationException(
                    "ValueAnnotatedTypeFactory: can't convert double to boolean");
        }
        return NumberUtils.castNumbers(newType, doubles);
    }

    /**
     * Returns a list with the same contents as its argument, but without duplicates. May return its
     * argument if its argument has no duplicates, but is not guaranteed to do so.
     *
     * @param <T> the type of elements in {@code values}
     * @param values a list of values
     * @return the values, with duplicates removed
     */
    public static <T extends Comparable<T>> List<T> removeDuplicates(List<T> values) {
        // This adds O(n) time cost, and has the benefit of sometimes avoiding allocating a TreeSet.
        if (Comparators.isInStrictOrder(values, Comparator.naturalOrder())) {
            return values;
        }

        Set<T> set = new TreeSet<>(values);
        if (values.size() == set.size()) {
            return values;
        } else {
            return new ArrayList<>(set);
        }
    }

    /**
     * Gets a list of lengths for a list of string values.
     *
     * @param values list of string values
     * @return list of unique lengths of strings in {@code values}
     */
    public static List<Integer> getLengthsForStringValues(List<String> values) {
        List<Integer> lengths = new ArrayList<>();
        for (String str : values) {
            lengths.add(str.length());
        }
        return ValueCheckerUtils.removeDuplicates(lengths);
    }

    /**
     * Gets the value field of an annotation with a list of strings in its value field. Null is
     * returned if the annotation has no value field.
     *
     * <p>For the Index Checker, this will get a list of array names from an Upper Bound or SameLen
     * annotation. making this safe to call on any Upper Bound or SameLen annotation.
     */
    public static List<String> getValueOfAnnotationWithStringArgument(AnnotationMirror anno) {
        if (!AnnotationUtils.hasElementValue(anno, "value")) {
            return null;
        }
        return AnnotationUtils.getElementValueArray(anno, "value", String.class, true);
    }

    /**
     * Returns a range representing the possible integral values represented by the passed {@code
     * AnnotatedTypeMirror}. If the passed {@code AnnotatedTypeMirror} does not contain an {@code
     * IntRange} annotation or an {@code IntVal} annotation, returns null.
     */
    public static Range getPossibleValues(
            AnnotatedTypeMirror valueType, ValueAnnotatedTypeFactory valueAnnotatedTypeFactory) {
        if (valueAnnotatedTypeFactory.isIntRange(valueType.getAnnotations())) {
            return valueAnnotatedTypeFactory.getRange(valueType.getAnnotation(IntRange.class));
        } else {
            List<Long> values =
                    ValueAnnotatedTypeFactory.getIntValues(valueType.getAnnotation(IntVal.class));
            if (values != null) {
                return Range.create(values);
            } else {
                return null;
            }
        }
    }

    /**
     * Either returns the exact value of the given tree according to the Constant Value Checker, or
     * null if the exact value is not known. This method should only be used by clients who need
     * exactly one value -- such as the LBC's binary operator rules -- and not by those that need to
     * know whether a valueType belongs to a particular qualifier.
     */
    public static Long getExactValue(Tree tree, ValueAnnotatedTypeFactory factory) {
        AnnotatedTypeMirror valueType = factory.getAnnotatedType(tree);
        Range possibleValues = getPossibleValues(valueType, factory);
        if (possibleValues != null && possibleValues.from == possibleValues.to) {
            return possibleValues.from;
        } else {
            return null;
        }
    }

    /**
     * Returns the exact value of an annotated element according to the Constant Value Checker, or
     * null if the exact value is not known.
     *
     * @param element the element to get the exact value from
     * @param factory ValueAnnotatedTypeFactory used for annotation accessing
     * @return the exact value of the element if it is constant, or null otherwise
     */
    public static Long getExactValue(Element element, ValueAnnotatedTypeFactory factory) {
        AnnotatedTypeMirror valueType = factory.getAnnotatedType(element);
        Range possibleValues = getPossibleValues(valueType, factory);
        if (possibleValues != null && possibleValues.from == possibleValues.to) {
            return possibleValues.from;
        } else {
            return null;
        }
    }

    /**
     * Either returns the exact string value of the given tree according to the Constant Value
     * Checker, or null if the exact value is not known. This method should only be used by clients
     * who need exactly one value and not by those that need to know whether a valueType belongs to
     * a particular qualifier.
     */
    public static String getExactStringValue(Tree tree, ValueAnnotatedTypeFactory factory) {
        AnnotatedTypeMirror valueType = factory.getAnnotatedType(tree);
        if (valueType.hasAnnotation(StringVal.class)) {
            AnnotationMirror valueAnno = valueType.getAnnotation(StringVal.class);
            List<String> possibleValues = getValueOfAnnotationWithStringArgument(valueAnno);
            if (possibleValues.size() == 1) {
                return possibleValues.get(0);
            }
        }
        return null;
    }

    /**
     * Finds the minimum value in a Value Checker type. If there is no information (such as when the
     * list of possible values is empty or null), returns null. Otherwise, returns the smallest
     * value in the list of possible values.
     */
    public static Long getMinValue(Tree tree, ValueAnnotatedTypeFactory factory) {
        AnnotatedTypeMirror valueType = factory.getAnnotatedType(tree);
        Range possibleValues = getPossibleValues(valueType, factory);
        if (possibleValues != null) {
            return possibleValues.from;
        } else {
            return null;
        }
    }

    /**
     * Finds the maximum value in a Value Checker type. If there is no information (such as when the
     * list of possible values is empty or null), returns null. Otherwise, returns the smallest
     * value in the list of possible values.
     */
    public static Long getMaxValue(Tree tree, ValueAnnotatedTypeFactory factory) {
        AnnotatedTypeMirror valueType = factory.getAnnotatedType(tree);
        Range possibleValues = getPossibleValues(valueType, factory);
        if (possibleValues != null) {
            return possibleValues.to;
        } else {
            return null;
        }
    }

    /**
     * Looks up the minlen of a member select tree. The tree must be an access to a sequence length.
     */
    public static Integer getMinLenFromTree(Tree tree, ValueAnnotatedTypeFactory valueATF) {
        AnnotatedTypeMirror minLenType = valueATF.getAnnotatedType(tree);
        Long min = valueATF.getMinimumIntegralValue(minLenType);
        if (min == null) {
            return null;
        }
        if (min < 0 || min > Integer.MAX_VALUE) {
            min = 0L;
        }
        return min.intValue();
    }

    /**
     * Queries the Value Checker to determine if there is a known minimum length for the array
     * represented by {@code tree}. If not, returns 0.
     */
    public static int getMinLen(Tree tree, ValueAnnotatedTypeFactory valueAnnotatedTypeFactory) {
        AnnotatedTypeMirror minLenType = valueAnnotatedTypeFactory.getAnnotatedType(tree);
        return valueAnnotatedTypeFactory.getMinLenValue(minLenType);
    }

    /**
     * Optimize the given JavaExpression, using Value Checker annotations. This implementation
     * replaces any expression that the factory has an exact value for, and does a small (not
     * exhaustive) amount of constant-folding as well.
     *
     * @param je the expression to optimize
     * @param factory the Valueannotatedtypefactory
     * @return an optimized version of the argument, or the argument if it cannot be optimized
     */
    // Could use a visitor instead, but this quick-and-dirty solution works.
    @SuppressWarnings("interning:not.interned") // ==  indicates whether `optimize` had any effect
    public static JavaExpression optimize(JavaExpression je, ValueAnnotatedTypeFactory factory) {

        if (je instanceof ArrayAccess) {
            ArrayAccess e = (ArrayAccess) je;
            JavaExpression optArray = optimize(e.getArray(), factory);
            JavaExpression optIndex = optimize(e.getIndex(), factory);
            if (e.getArray() == optArray && e.getIndex() == optIndex) {
                return e;
            } else {
                return new ArrayAccess(e.getType(), optArray, optIndex);
            }

        } else if (je instanceof ArrayCreation) {
            ArrayCreation e = (ArrayCreation) je;
            List<JavaExpression> optDimensions = optimize(e.getDimensions(), factory);
            List<JavaExpression> optInializers = optimize(e.getInitializers(), factory);
            if (e.getDimensions() == optDimensions && e.getInitializers() == optInializers) {
                return e;
            } else {
                return new ArrayCreation(e.getType(), optDimensions, optInializers);
            }

        } else if (je instanceof BinaryOperation) {
            BinaryOperation e = (BinaryOperation) je;
            JavaExpression optLeft = optimize(e.getLeft(), factory);
            JavaExpression optRight = optimize(e.getRight(), factory);
            if (e.getLeft() == optLeft && e.getRight() == optRight) {
                return e;
            } else {
                return new BinaryOperation(e.getType(), e.getOperationKind(), optLeft, optRight);
            }

        } else if (je instanceof ClassName) {
            return je;

        } else if (je instanceof FieldAccess) {
            FieldAccess e = (FieldAccess) je;
            JavaExpression optReceiver = optimize(e.getReceiver(), factory);
            if (e.getReceiver() == optReceiver) {
                return e;
            } else {
                return new FieldAccess(optReceiver, e.getType(), e.getField());
            }

        } else if (je instanceof LocalVariable) {
            LocalVariable e = (LocalVariable) je;
            Element element = e.getElement();
            Long exactValue = ValueCheckerUtils.getExactValue(element, factory);
            if (exactValue != null) {
                return new ValueLiteral(e.getType(), exactValue.intValue());
            } else {
                return e;
            }

        } else if (je instanceof MethodCall) {
            MethodCall e = (MethodCall) je;
            JavaExpression optReceiver = optimize(e.getReceiver(), factory);
            List<JavaExpression> optArguments = optimize(e.getArguments(), factory);
            if (e.getReceiver() == optReceiver && e.getArguments() == optArguments) {
                return e;
            } else {
                return new MethodCall(e.getType(), e.getElement(), optReceiver, optArguments);
            }

        } else if (je instanceof ThisReference) {
            ThisReference e = (ThisReference) je;
            return e;

        } else if (je instanceof UnaryOperation) {
            UnaryOperation e = (UnaryOperation) je;
            JavaExpression optOperand = optimize(e.getOperand(), factory);
            if (e.getOperand() == optOperand) {
                return e;
            } else {
                return new UnaryOperation(e.getType(), e.getOperationKind(), optOperand);
            }

        } else if (je instanceof Unknown) {
            Unknown e = (Unknown) je;
            return e;

        } else if (je instanceof ValueLiteral) {
            ValueLiteral e = (ValueLiteral) je;
            return e;

        } else {
            throw new BugInCF("Unhandled JavaExpression %s %s", je.getClass(), je);
        }
    }

    /**
     * Optimize the given list of JavaExpressions, using Value Checker annotations.
     *
     * @param list the expressions to optimize
     * @param factory the Valueannotatedtypefactory
     * @return an optimized version of the argument, or the argument if nothing in it can be
     *     optimized
     */
    @SuppressWarnings("interning:not.interned") // ==  indicates whether `optimize` had any effect
    private static List<JavaExpression> optimize(
            List<JavaExpression> list, ValueAnnotatedTypeFactory factory) {
        boolean changed = false;
        List<JavaExpression> result = new ArrayList<>(list.size());
        for (JavaExpression je : list) {
            JavaExpression opt = optimize(je, factory);
            changed = changed || (je != opt);
            result.add(opt);
        }
        if (changed) {
            return result;
        } else {
            return list;
        }
    }
}
