package org.checkerframework.common.value.util;

import org.checkerframework.javacutil.TypeKindUtils;
import org.plumelib.util.CollectionsPlume;

import java.util.List;

import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

/** Utility routines for manipulating numbers. */
public class NumberUtils {

    /** Converts a {@code List<A>} to a {@code List<B>}, where A and B are numeric types. */
    public static List<? extends Number> castNumbers(
            TypeMirror type, List<? extends Number> numbers) {
        if (numbers == null) {
            return null;
        }
        TypeKind typeKind = TypeKindUtils.primitiveOrBoxedToTypeKind(type);
        if (typeKind == null) {
            throw new UnsupportedOperationException(type.toString());
        }
        switch (typeKind) {
            case BYTE:
                return CollectionsPlume.mapList(Number::byteValue, numbers);
            case CHAR:
                return CollectionsPlume.mapList(Number::intValue, numbers);
            case DOUBLE:
                return CollectionsPlume.mapList(Number::doubleValue, numbers);
            case FLOAT:
                return CollectionsPlume.mapList(Number::floatValue, numbers);
            case INT:
                return CollectionsPlume.mapList(Number::intValue, numbers);
            case LONG:
                return CollectionsPlume.mapList(Number::longValue, numbers);
            case SHORT:
                return CollectionsPlume.mapList(Number::shortValue, numbers);
            default:
                throw new UnsupportedOperationException(typeKind + ": " + type);
        }
    }

    /**
     * Return a range that restricts the given range to the given type. That is, return the range
     * resulting from casting a value with the given range.
     *
     * @param type the type for the cast; the result will be within it
     * @param range the original range; the result will be within it
     * @return the intersection of the given range and the possible values of the given type
     */
    public static Range castRange(TypeMirror type, Range range) {
        TypeKind typeKind = TypeKindUtils.primitiveOrBoxedToTypeKind(type);
        if (typeKind == null) {
            throw new UnsupportedOperationException(type.toString());
        }
        switch (typeKind) {
            case BYTE:
                return range.byteRange();
            case CHAR:
                return range.charRange();
            case SHORT:
                return range.shortRange();
            case INT:
                return range.intRange();
            case LONG:
            case FLOAT:
            case DOUBLE:
                return range;
            default:
                throw new UnsupportedOperationException(typeKind + ": " + type);
        }
    }
}
