package org.checkerframework.javacutil;

import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

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
    public static TypeKind primitiveOrBoxedToTypeKind(TypeMirror type) {
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

    // TODO: I don't want this.  Just have this structure in the implementation of
    // SignatureAnnotatedTreeFactory's getWidenedAnnotations.
    public static boolean isNarrower(TypeKind tk1, TypeKind tk2) {
        switch (tk1) {
            case BYTE:
                switch (tk2) {
                    case BYTE:
                        return false;
                    case SHORT:
                    case CHAR:
                    case INT:
                    case LONG:
                        return true;
                    default:
                        throw new Error("Non-primitive typekind " + tk2);
                }
            case SHORT:
                switch (tk2) {
                    case BYTE:
                    case SHORT:
                    case CHAR:
                        return false;
                    case INT:
                    case LONG:
                        return true;
                    default:
                        throw new Error("Non-primitive typekind " + tk2);
                }
            case CHAR:
                switch (tk2) {
                    case BYTE:
                    case SHORT:
                    case CHAR:
                        return false;
                    case INT:
                    case LONG:
                        return true;
                    default:
                        throw new Error("Non-primitive typekind " + tk2);
                }
            case INT:
                switch (tk2) {
                    case BYTE:
                    case SHORT:
                    case CHAR:
                    case INT:
                        return false;
                    case LONG:
                        return true;
                    default:
                        throw new Error("Non-primitive typekind " + tk2);
                }
            case LONG:
                switch (tk2) {
                    case BYTE:
                    case SHORT:
                    case CHAR:
                    case INT:
                    case LONG:
                        return false;
                    default:
                        throw new Error("Non-primitive typekind " + tk2);
                }
            default:
                throw new Error("Non-primitive typekind " + tk1);
        }
    }
}
