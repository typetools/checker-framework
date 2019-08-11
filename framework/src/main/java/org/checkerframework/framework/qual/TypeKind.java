package org.checkerframework.framework.qual;

/**
 * Specifies kinds of types.
 *
 * <p>These correspond to the constants in {@link javax.lang.model.type.TypeKind}. However, that
 * enum is not available on Android and a warning is produced. So this enum is used instead.
 *
 * @checker_framework.manual #creating-type-introduction Declaratively specifying default
 *     annotations
 */
public enum TypeKind {
    /** Corresponds to {@link javax.lang.model.type.TypeKind#BOOLEAN} types. */
    BOOLEAN,

    /** Corresponds to {@link javax.lang.model.type.TypeKind#BYTE} types. */
    BYTE,

    /** Corresponds to {@link javax.lang.model.type.TypeKind#SHORT} types. */
    SHORT,

    /** Corresponds to {@link javax.lang.model.type.TypeKind#INT} types. */
    INT,

    /** Corresponds to {@link javax.lang.model.type.TypeKind#LONG} types. */
    LONG,

    /** Corresponds to {@link javax.lang.model.type.TypeKind#CHAR} types. */
    CHAR,

    /** Corresponds to {@link javax.lang.model.type.TypeKind#FLOAT} types. */
    FLOAT,

    /** Corresponds to {@link javax.lang.model.type.TypeKind#DOUBLE} types. */
    DOUBLE,

    /** Corresponds to {@link javax.lang.model.type.TypeKind#VOID} types. */
    VOID,

    /** Corresponds to {@link javax.lang.model.type.TypeKind#NONE} types. */
    NONE,

    /** Corresponds to {@link javax.lang.model.type.TypeKind#NULL} types. */
    NULL,

    /** Corresponds to {@link javax.lang.model.type.TypeKind#ARRAY} types. */
    ARRAY,

    /** Corresponds to {@link javax.lang.model.type.TypeKind#DECLARED} types. */
    DECLARED,

    /** Corresponds to {@link javax.lang.model.type.TypeKind#ERROR} types. */
    ERROR,

    /** Corresponds to {@link javax.lang.model.type.TypeKind#TYPEVAR} types. */
    TYPEVAR,

    /** Corresponds to {@link javax.lang.model.type.TypeKind#WILDCARD} types. */
    WILDCARD,

    /** Corresponds to {@link javax.lang.model.type.TypeKind#PACKAGE} types. */
    PACKAGE,

    /** Corresponds to {@link javax.lang.model.type.TypeKind#EXECUTABLE} types. */
    EXECUTABLE,

    /** Corresponds to {@link javax.lang.model.type.TypeKind#OTHER} types. */
    OTHER,

    /** Corresponds to {@link javax.lang.model.type.TypeKind#UNION} types. */
    UNION,

    /** Corresponds to {@link javax.lang.model.type.TypeKind#INTERSECTION} types. */
    INTERSECTION;
}
