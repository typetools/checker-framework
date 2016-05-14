package org.checkerframework.common.value;

import org.checkerframework.common.value.qual.ArrayLen;
import org.checkerframework.common.value.qual.BoolVal;
import org.checkerframework.common.value.qual.BottomVal;
import org.checkerframework.common.value.qual.DoubleVal;
import org.checkerframework.common.value.qual.IntVal;
import org.checkerframework.common.value.qual.StringVal;
import org.checkerframework.common.value.qual.UnknownVal;
import org.checkerframework.common.value.util.NumberUtils;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TypesUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;


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
            String stringType = TypesUtils.getQualifiedName(
                    (DeclaredType) type).toString();
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

    public static List<?> getValuesCastedToType(AnnotationMirror anno, TypeMirror castTo) {
        Class<?> castType = ValueCheckerUtils.getClassFromType(castTo);
        List<?> values = null;

        if (AnnotationUtils.areSameByClass(anno, DoubleVal.class)) {
            values = convertDoubleVal(anno, castType, castTo);
        } else if (AnnotationUtils.areSameByClass(anno, IntVal.class)) {
            values = convertIntVal(anno, castType, castTo);
        } else if (AnnotationUtils.areSameByClass(anno, StringVal.class)) {
            values = convertStringVal(anno, castType);
        } else if (AnnotationUtils.areSameByClass(anno, BoolVal.class)) {
            values = convertBoolVal(anno, castType);
        } else if (AnnotationUtils.areSameByClass(anno, BottomVal.class)) {
            values = convertBottomVal(anno, castType);
        } else if (AnnotationUtils.areSameByClass(anno, UnknownVal.class) ||
                AnnotationUtils.areSameByClass(anno, ArrayLen.class)) {
            values = new ArrayList<>();
        }
        return values;
    }

    private static List<?> convertBottomVal(AnnotationMirror anno,
            Class<?> newClass) {
        if (newClass == String.class) {
            return Collections.singletonList("null");
        } else {
            return new ArrayList<>();
        }
    }

    private static List<?> convertToStringVal(List<?> origValues) {
        List<String> strings = new ArrayList<>();
        for (Object value : origValues) {
            strings.add(value.toString());
        }
        return strings;
    }

    private static List<?> convertBoolVal(AnnotationMirror anno, Class<?> newClass) {
        List<Boolean> bools = AnnotationUtils.getElementValueArray(anno,
                "value", Boolean.class, true);

        if (newClass == String.class) {
            return convertToStringVal(bools);
        }
        return bools;
    }

    private static List<?> convertStringVal(AnnotationMirror anno,
            Class<?> newClass) {
        List<String> strings = AnnotationUtils.getElementValueArray(anno,
                "value", String.class, true);

        if (newClass == byte[].class) {
            List<byte[]> bytes = new ArrayList<>();
            for (String s : strings) {
                bytes.add(s.getBytes());
            }
            return bytes;
        } else if (newClass == char[].class) {
            List<char[]> chars = new ArrayList<>();
            for (String s : strings) {
                chars.add(s.toCharArray());
            }
            return chars;
        } else if (newClass == Object.class && strings.size() == 1) {
            if (strings.get(0).equals("null")) {
                return strings;
            }
        }
        return strings;
    }

    private static List<?> convertIntVal(AnnotationMirror anno, Class<?> newClass, TypeMirror newType) {
        List<Long> longs = ValueAnnotatedTypeFactory.getIntValues(anno);

        if (newClass == String.class) {
            return convertToStringVal(longs);
        } else if (newClass == Character.class || newClass == char.class) {
            List<Character> chars = new ArrayList<>();
            for (Long l : longs) {
                chars.add((char) l.longValue());
            }
            return chars;
        } else if (newClass == Boolean.class) {
            throw new UnsupportedOperationException("ValueAnnotatedTypeFactory: can't convert double to boolean");
        }
        return NumberUtils.castNumbers(newType, longs);
    }

    private static List<?> convertDoubleVal(AnnotationMirror anno,
            Class<?> newClass, TypeMirror newType) {
        List<Double> doubles = ValueAnnotatedTypeFactory.getDoubleValues(anno);
        if (newClass == String.class) {
            return convertToStringVal(doubles);
        } else if (newClass == Character.class || newClass == char.class) {
            List<Character> chars = new ArrayList<>();
            for (Double l : doubles) {
                chars.add((char) l.doubleValue());
            }
            return chars;
        } else if (newClass == Boolean.class) {
            throw new UnsupportedOperationException("ValueAnnotatedTypeFactory: can't convert double to boolean");
        }
        return NumberUtils.castNumbers(newType, doubles);
    }

    public static <T extends Comparable<T>> List<T> removeDuplicates(List<T> values) {
        Set<T> set = new TreeSet<>(values);
        return new ArrayList<T>(set);

    }
}
