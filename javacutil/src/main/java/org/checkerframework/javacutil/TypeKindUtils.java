package org.checkerframework.javacutil;

import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.Nullable;

/** A utility class that helps with {@link TypeKind}s. */
public final class TypeKindUtils {

    /** This class cannot be instantiated. */
    private TypeKindUtils() {
        throw new AssertionError("Class TypeKindUtils cannot be instantiated.");
    }

    /**
     * Return true if the argument is one of INT, SHORT, BYTE, CHAR, LONG.
     *
     * @param typeKind the TypeKind to inspect
     * @return true if typeKind is a primitive integral type kind
     */
    public static boolean isIntegral(TypeKind typeKind) {
        switch (typeKind) {
            case INT:
            case SHORT:
            case BYTE:
            case CHAR:
            case LONG:
                return true;
            default:
                return false;
        }
    }

    /**
     * Return true if the argument is one of FLOAT, DOUBLE.
     *
     * @param typeKind the TypeKind to inspect
     * @return true if typeKind is a primitive floating point type kind
     */
    public static boolean isFloatingPoint(TypeKind typeKind) {
        switch (typeKind) {
            case FLOAT:
            case DOUBLE:
                return true;
            default:
                return false;
        }
    }

    /**
     * Returns true iff the argument is a primitive numeric type kind.
     *
     * @param typeKind a type kind
     * @return true if the argument is a primitive numeric type kind
     */
    public static boolean isNumeric(TypeKind typeKind) {
        switch (typeKind) {
            case BYTE:
            case CHAR:
            case DOUBLE:
            case FLOAT:
            case INT:
            case LONG:
            case SHORT:
                return true;
            default:
                return false;
        }
    }

    // Cannot create an overload that takes an AnnotatedTypeMirror.java because javacutil
    // must not depend on the framework package.
    /**
     * Given a primitive type, return its kind. Given a boxed primitive type, return the
     * corresponding primitive type kind. Otherwise, return null.
     *
     * @param type a primitive or boxed primitive type
     * @return a primitive type kind, or null
     */
    public static @Nullable TypeKind primitiveOrBoxedToTypeKind(TypeMirror type) {
        final TypeKind typeKind = type.getKind();
        if (typeKind.isPrimitive()) {
            return typeKind;
        }

        if (!(type instanceof DeclaredType)) {
            return null;
        }

        final String typeString = TypesUtils.getQualifiedName((DeclaredType) type).toString();

        switch (typeString) {
            case "java.lang.Byte":
                return TypeKind.BYTE;
            case "java.lang.Boolean":
                return TypeKind.BOOLEAN;
            case "java.lang.Character":
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
            default:
                // TODO: this method should only be called for primitive or boxed primitive types.
                // However, it is also used to implement other methods where this condition might
                // not be met.
                // Think of a nicer way to structure all these methods.
                // throw new BugInCF("Expected primitive wrapper, got " + type + " kind: " +
                // typeKind);
                return typeKind;
        }
    }

    // No overload that takes AnnotatedTypeMirror becasue javacutil cannot depend on framework.
    /**
     * Returns the widened numeric type for an arithmetic operation performed on a value of the left
     * type and the right type. Defined in JLS 5.6.2. We return a {@link TypeKind} because creating
     * a {@link TypeMirror} requires a {@link javax.lang.model.util.Types} object from the {@link
     * javax.annotation.processing.ProcessingEnvironment}.
     *
     * @param left a type mirror
     * @param right a type mirror
     * @return the result of widening numeric conversion, or NONE when the conversion cannot be
     *     performed
     */
    public static TypeKind widenedNumericType(TypeMirror left, TypeMirror right) {
        return TypeKindUtils.widenedNumericType(left.getKind(), right.getKind());
    }

    /**
     * Given two type kinds, return the type kind they are widened to, when an arithmetic operation
     * is performed on them. Defined in JLS 5.6.2.
     *
     * @param a a type kind
     * @param b a type kind
     * @return the type kind to which they are widened, when an operation is performed on them
     */
    public static TypeKind widenedNumericType(TypeKind a, TypeKind b) {
        if (!isNumeric(a) || !isNumeric(b)) {
            return TypeKind.NONE;
        }

        if (a == TypeKind.DOUBLE || b == TypeKind.DOUBLE) {
            return TypeKind.DOUBLE;
        }

        if (a == TypeKind.FLOAT || b == TypeKind.FLOAT) {
            return TypeKind.FLOAT;
        }

        if (a == TypeKind.LONG || b == TypeKind.LONG) {
            return TypeKind.LONG;
        }

        return TypeKind.INT;
    }

    /**
     * Returns true if both types are integral and the first type is strictly narrower (represented
     * by fewer bits) than the second type.
     *
     * @param a a primitive type
     * @param b a primitive type
     * @return true if {@code a} is represented by fewer bits than {@code b}
     */
    public static boolean isNarrowerIntegral(TypeKind a, TypeKind b) {
        int aBits = numIntegralBits(a);
        if (aBits == -1) {
            return false;
        }
        int bBits = numIntegralBits(b);
        if (bBits == -1) {
            return false;
        }
        return aBits < bBits;
    }

    /**
     * Returns the number of bits in the representation of an integral primitive type. Returns -1 if
     * the type is not an integral primitive type.
     *
     * @param tk a primitive type kind
     * @return the number of bits in its representation, or -1 if not integral
     */
    private static int numIntegralBits(TypeKind tk) {
        switch (tk) {
            case BYTE:
                return 8;
            case SHORT:
                return 16;
            case CHAR:
                return 16;
            case INT:
                return 32;
            case LONG:
                return 64;
            case BOOLEAN:
            case DOUBLE:
            case FLOAT:
            default:
                return -1;
        }
    }
}
