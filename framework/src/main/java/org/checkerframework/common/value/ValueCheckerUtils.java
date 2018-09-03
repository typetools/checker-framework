package org.checkerframework.common.value;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.common.value.qual.ArrayLen;
import org.checkerframework.common.value.qual.BoolVal;
import org.checkerframework.common.value.qual.BottomVal;
import org.checkerframework.common.value.qual.DoubleVal;
import org.checkerframework.common.value.qual.IntRange;
import org.checkerframework.common.value.qual.IntVal;
import org.checkerframework.common.value.qual.StringVal;
import org.checkerframework.common.value.qual.UnknownVal;
import org.checkerframework.common.value.util.NumberUtils;
import org.checkerframework.common.value.util.Range;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TypesUtils;

public class ValueCheckerUtils {
    public static Class<?> getClassFromType(TypeMirror type) {

        switch (type.getKind()) {
            case INT:
                return int.class;
            case LONG:
                return long.class;
            case SHORT:
                return short.class;
            case BYTE:
                return byte.class;
            case CHAR:
                return char.class;
            case DOUBLE:
                return double.class;
            case FLOAT:
                return float.class;
            case BOOLEAN:
                return boolean.class;
            case ARRAY:
                return getArrayClassObject(((ArrayType) type).getComponentType());
            case DECLARED:
                String stringType = TypesUtils.getQualifiedName((DeclaredType) type).toString();
                if (stringType.equals("<nulltype>")) {
                    return Object.class;
                }

                try {
                    return Class.forName(stringType);
                } catch (ClassNotFoundException | UnsupportedClassVersionError e) {
                    return Object.class;
                }

            default:
                return Object.class;
        }
    }

    public static Class<?> getArrayClassObject(TypeMirror componentType) {
        switch (componentType.getKind()) {
            case INT:
                return int[].class;
            case LONG:
                return long[].class;
            case SHORT:
                return short[].class;
            case BYTE:
                return byte[].class;
            case CHAR:
                return char[].class;
            case DOUBLE:
                return double[].class;
            case FLOAT:
                return float[].class;
            case BOOLEAN:
                return boolean[].class;
            default:
                return Object[].class;
        }
    }

    /**
     * Get a list of values of annotation, and then cast them to a given type.
     *
     * @param anno the annotation that contains values
     * @param castTo the type that is casted to
     * @return a list of values after the casting
     */
    public static List<?> getValuesCastedToType(AnnotationMirror anno, TypeMirror castTo) {
        Class<?> castType = ValueCheckerUtils.getClassFromType(castTo);
        List<?> values = null;

        if (AnnotationUtils.areSameByClass(anno, DoubleVal.class)) {
            values = convertDoubleVal(anno, castType, castTo);
        } else if (AnnotationUtils.areSameByClass(anno, IntVal.class)) {
            List<Long> longs = ValueAnnotatedTypeFactory.getIntValues(anno);
            values = convertIntVal(longs, castType, castTo);
        } else if (AnnotationUtils.areSameByClass(anno, IntRange.class)) {
            Range range = ValueAnnotatedTypeFactory.getRange(anno);
            List<Long> longs = getValuesFromRange(range, Long.class);
            values = convertIntVal(longs, castType, castTo);
        } else if (AnnotationUtils.areSameByClass(anno, StringVal.class)) {
            values = convertStringVal(anno, castType);
        } else if (AnnotationUtils.areSameByClass(anno, BoolVal.class)) {
            values = convertBoolVal(anno, castType);
        } else if (AnnotationUtils.areSameByClass(anno, BottomVal.class)) {
            values = new ArrayList<>();
        } else if (AnnotationUtils.areSameByClass(anno, UnknownVal.class)) {
            values = null;
        } else if (AnnotationUtils.areSameByClass(anno, ArrayLen.class)) {
            values = new ArrayList<>();
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
        // The number elements in the values list should not exceed MAX_VALUES (10).
        List<Long> longValues = new ArrayList<>();
        for (Number value : values) {
            longValues.add(value.longValue());
        }
        return new Range(Collections.min(longValues), Collections.max(longValues));
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

    public static <T extends Comparable<T>> List<T> removeDuplicates(List<T> values) {
        Set<T> set = new TreeSet<>(values);
        return new ArrayList<>(set);
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
}
