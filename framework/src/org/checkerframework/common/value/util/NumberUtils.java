package org.checkerframework.common.value.util;

import java.util.ArrayList;
import java.util.List;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.javacutil.TypesUtils;

public class NumberUtils {

    /** Converts a {@code List<A>} to a {@code List<B>}, where A and B are numeric types. */
    public static List<? extends Number> castNumbers(
            TypeMirror type, List<? extends Number> numbers) {
        TypeKind typeKind = unBoxPrimitive(type);
        switch (typeKind) {
            case BYTE:
                List<Byte> bytes = new ArrayList<>();
                for (Number l : numbers) {
                    bytes.add(l.byteValue());
                }
                return bytes;
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
                throw new UnsupportedOperationException(typeKind.toString());
        }
    }

    public static Range castRange(TypeMirror type, Range range) {
        TypeKind typeKind = unBoxPrimitive(type);
        switch (typeKind) {
            case INT:
                return range.intRange();
            case SHORT:
                return range.shortRange();
            case BYTE:
                return range.byteRange();
            case LONG:
            case CHAR:
            case FLOAT:
            case DOUBLE:
                return range;
            default:
                throw new UnsupportedOperationException(typeKind.toString());
        }
    }

    private static TypeKind unBoxPrimitive(TypeMirror type) {
        if (type.getKind() == TypeKind.DECLARED) {
            String stringType = TypesUtils.getQualifiedName((DeclaredType) type).toString();

            switch (stringType) {
                case "java.lang.Byte":
                    return TypeKind.BYTE;
                case "java.lang.Boolean":
                    return TypeKind.BOOLEAN;
                case "java.lang.Charater":
                    return TypeKind.CHAR;
                case "java.lang.Double":
                    return TypeKind.DOUBLE;
                case "java.lang.Float":
                    return TypeKind.FLOAT;
                case "java.lang.Integer":
                    return TypeKind.INT;
                case "java.lang.Long":
                    return TypeKind.LONG;
                case "java.lang.Short":
                    return TypeKind.SHORT;
            }
        }
        return type.getKind();
    }
}
