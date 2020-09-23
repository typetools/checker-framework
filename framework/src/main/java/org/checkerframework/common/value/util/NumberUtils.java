package org.checkerframework.common.value.util;

import java.util.ArrayList;
import java.util.List;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.javacutil.TypeKindUtils;

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
                List<Byte> bytes = new ArrayList<>();
                for (Number l : numbers) {
                    bytes.add(l.byteValue());
                }
                return bytes;
            case CHAR:
                List<Integer> chars = new ArrayList<>();
                for (Number l : numbers) {
                    chars.add(l.intValue());
                }
                return chars;
            case DOUBLE:
                List<Double> doubles = new ArrayList<>();
                for (Number l : numbers) {
                    doubles.add(l.doubleValue());
                }
                return doubles;
            case FLOAT:
                List<Float> floats = new ArrayList<>();
                for (Number l : numbers) {
                    floats.add(l.floatValue());
                }
                return floats;
            case INT:
                List<Integer> ints = new ArrayList<>();
                for (Number l : numbers) {
                    ints.add(l.intValue());
                }
                return ints;
            case LONG:
                List<Long> longs = new ArrayList<>();
                for (Number l : numbers) {
                    longs.add(l.longValue());
                }
                return longs;
            case SHORT:
                List<Short> shorts = new ArrayList<>();
                for (Number l : numbers) {
                    shorts.add(l.shortValue());
                }
                return shorts;
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
