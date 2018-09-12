package org.checkerframework.framework.util;

import static org.checkerframework.framework.util.AtmKind.ARRAY;
import static org.checkerframework.framework.util.AtmKind.DECLARED;
import static org.checkerframework.framework.util.AtmKind.EXECUTABLE;
import static org.checkerframework.framework.util.AtmKind.INTERSECTION;
import static org.checkerframework.framework.util.AtmKind.NONE;
import static org.checkerframework.framework.util.AtmKind.NULL;
import static org.checkerframework.framework.util.AtmKind.PRIMITIVE;
import static org.checkerframework.framework.util.AtmKind.TYPEVAR;
import static org.checkerframework.framework.util.AtmKind.UNION;
import static org.checkerframework.framework.util.AtmKind.WILDCARD;

import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedIntersectionType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedNoType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedNullType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedPrimitiveType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedUnionType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;
import org.checkerframework.framework.type.visitor.AtmComboVisitor;
import org.checkerframework.javacutil.BugInCF;

/**
 * AtmKind should mirror TypeKind except that each member has a reference to the AnnotatedTypeMirror
 * that would represents types of its kind.
 *
 * <p>Note: This class is only useful so that AtmCombo can look up combinations via the
 * AtmKind.ordinal(). See AtmCombo.comboMap
 */
enum AtmKind {
    ARRAY(AnnotatedArrayType.class),
    DECLARED(AnnotatedDeclaredType.class),
    EXECUTABLE(AnnotatedExecutableType.class),
    INTERSECTION(AnnotatedIntersectionType.class),
    NONE(AnnotatedNoType.class),
    NULL(AnnotatedNullType.class),
    PRIMITIVE(AnnotatedPrimitiveType.class),
    TYPEVAR(AnnotatedTypeVariable.class),
    UNION(AnnotatedUnionType.class),
    WILDCARD(AnnotatedWildcardType.class);

    // The AnnotatedTypeMirror subclass that represents types of this kind
    public final Class<? extends AnnotatedTypeMirror> atmClass;

    AtmKind(Class<? extends AnnotatedTypeMirror> atmClass) {
        this.atmClass = atmClass;
    }

    /** @return the AtmKind corresponding to the class of atm */
    public static AtmKind valueOf(final AnnotatedTypeMirror atm) {
        final Class<?> argClass = atm.getClass();

        for (AtmKind atmKind : AtmKind.values()) {
            final Class<?> kindClass = atmKind.atmClass;
            if (argClass.equals(kindClass)) {
                return atmKind;
            }
        }

        throw new BugInCF("Unhandled AnnotatedTypeMirror ( " + atm.getClass() + " )");
    }
}

/**
 * An enum representing the cartesian product of the set of AtmKinds with itself. This represents
 * all pair-wise combinations of AnnotatedTypeMirror subclasses. AtmCombo can be used in a switch to
 * easily (and in a readable fashion) enumerate a subset of Atm pairs to handle. It is also used to
 * execute AtmComboVisitor, which is a visitor of all possible combinations of AnnotatedTypeMirror
 * subclasses.
 *
 * <p>For example:
 *
 * <pre>{@code
 * switch (AtmCombo.valueOf(atm1, atm2)) {
 *     case WILDCARD_WILDCARD:
 *     case TYPEVAR_TYPEVAR:
 *         doSomething(atm1, atm2);
 *         break;
 * }
 * }</pre>
 *
 * @see AtmCombo#accept
 */
public enum AtmCombo {
    ARRAY_ARRAY(ARRAY, ARRAY),
    ARRAY_DECLARED(ARRAY, DECLARED),
    ARRAY_EXECUTABLE(ARRAY, EXECUTABLE),
    ARRAY_INTERSECTION(ARRAY, INTERSECTION),
    ARRAY_NONE(ARRAY, NONE),
    ARRAY_NULL(ARRAY, NULL),
    ARRAY_PRIMITIVE(ARRAY, PRIMITIVE),
    ARRAY_UNION(ARRAY, UNION),
    ARRAY_TYPEVAR(ARRAY, TYPEVAR),
    ARRAY_WILDCARD(ARRAY, WILDCARD),

    DECLARED_ARRAY(DECLARED, ARRAY),
    DECLARED_DECLARED(DECLARED, DECLARED),
    DECLARED_EXECUTABLE(DECLARED, EXECUTABLE),
    DECLARED_INTERSECTION(DECLARED, INTERSECTION),
    DECLARED_NONE(DECLARED, NONE),
    DECLARED_NULL(DECLARED, NULL),
    DECLARED_PRIMITIVE(DECLARED, PRIMITIVE),
    DECLARED_TYPEVAR(DECLARED, TYPEVAR),
    DECLARED_UNION(DECLARED, UNION),
    DECLARED_WILDCARD(DECLARED, WILDCARD),

    EXECUTABLE_ARRAY(EXECUTABLE, ARRAY),
    EXECUTABLE_DECLARED(EXECUTABLE, DECLARED),
    EXECUTABLE_EXECUTABLE(EXECUTABLE, EXECUTABLE),
    EXECUTABLE_INTERSECTION(EXECUTABLE, INTERSECTION),
    EXECUTABLE_NONE(EXECUTABLE, NONE),
    EXECUTABLE_NULL(EXECUTABLE, NULL),
    EXECUTABLE_PRIMITIVE(EXECUTABLE, PRIMITIVE),
    EXECUTABLE_TYPEVAR(EXECUTABLE, TYPEVAR),
    EXECUTABLE_UNION(EXECUTABLE, UNION),
    EXECUTABLE_WILDCARD(EXECUTABLE, WILDCARD),

    INTERSECTION_ARRAY(INTERSECTION, ARRAY),
    INTERSECTION_DECLARED(INTERSECTION, DECLARED),
    INTERSECTION_EXECUTABLE(INTERSECTION, EXECUTABLE),
    INTERSECTION_INTERSECTION(INTERSECTION, INTERSECTION),
    INTERSECTION_NONE(INTERSECTION, NONE),
    INTERSECTION_NULL(INTERSECTION, NULL),
    INTERSECTION_PRIMITIVE(INTERSECTION, PRIMITIVE),
    INTERSECTION_TYPEVAR(INTERSECTION, TYPEVAR),
    INTERSECTION_UNION(INTERSECTION, UNION),
    INTERSECTION_WILDCARD(INTERSECTION, WILDCARD),

    NONE_ARRAY(NONE, ARRAY),
    NONE_DECLARED(NONE, DECLARED),
    NONE_EXECUTABLE(NONE, EXECUTABLE),
    NONE_INTERSECTION(NONE, INTERSECTION),
    NONE_NONE(NONE, NONE),
    NONE_NULL(NONE, NULL),
    NONE_PRIMITIVE(NONE, PRIMITIVE),
    NONE_TYPEVAR(NONE, TYPEVAR),
    NONE_UNION(NONE, UNION),
    NONE_WILDCARD(NONE, WILDCARD),

    NULL_ARRAY(NULL, ARRAY),
    NULL_DECLARED(NULL, DECLARED),
    NULL_EXECUTABLE(NULL, EXECUTABLE),
    NULL_INTERSECTION(NULL, INTERSECTION),
    NULL_NONE(NULL, NONE),
    NULL_NULL(NULL, NULL),
    NULL_PRIMITIVE(NULL, PRIMITIVE),
    NULL_TYPEVAR(NULL, TYPEVAR),
    NULL_UNION(NULL, UNION),
    NULL_WILDCARD(NULL, WILDCARD),

    PRIMITIVE_ARRAY(PRIMITIVE, ARRAY),
    PRIMITIVE_DECLARED(PRIMITIVE, DECLARED),
    PRIMITIVE_EXECUTABLE(PRIMITIVE, EXECUTABLE),
    PRIMITIVE_INTERSECTION(PRIMITIVE, INTERSECTION),
    PRIMITIVE_NONE(PRIMITIVE, NONE),
    PRIMITIVE_NULL(PRIMITIVE, NULL),
    PRIMITIVE_PRIMITIVE(PRIMITIVE, PRIMITIVE),
    PRIMITIVE_TYPEVAR(PRIMITIVE, TYPEVAR),
    PRIMITIVE_UNION(PRIMITIVE, UNION),
    PRIMITIVE_WILDCARD(PRIMITIVE, WILDCARD),

    TYPEVAR_ARRAY(TYPEVAR, ARRAY),
    TYPEVAR_DECLARED(TYPEVAR, DECLARED),
    TYPEVAR_EXECUTABLE(TYPEVAR, EXECUTABLE),
    TYPEVAR_INTERSECTION(TYPEVAR, INTERSECTION),
    TYPEVAR_NONE(TYPEVAR, NONE),
    TYPEVAR_NULL(TYPEVAR, NULL),
    TYPEVAR_PRIMITIVE(TYPEVAR, PRIMITIVE),
    TYPEVAR_TYPEVAR(TYPEVAR, TYPEVAR),
    TYPEVAR_UNION(TYPEVAR, UNION),
    TYPEVAR_WILDCARD(TYPEVAR, WILDCARD),

    UNION_ARRAY(UNION, ARRAY),
    UNION_DECLARED(UNION, DECLARED),
    UNION_EXECUTABLE(UNION, EXECUTABLE),
    UNION_INTERSECTION(UNION, INTERSECTION),
    UNION_NONE(UNION, NONE),
    UNION_NULL(UNION, NULL),
    UNION_PRIMITIVE(UNION, PRIMITIVE),
    UNION_TYPEVAR(UNION, TYPEVAR),
    UNION_UNION(UNION, UNION),
    UNION_WILDCARD(UNION, WILDCARD),

    WILDCARD_ARRAY(WILDCARD, ARRAY),
    WILDCARD_DECLARED(WILDCARD, DECLARED),
    WILDCARD_EXECUTABLE(WILDCARD, EXECUTABLE),
    WILDCARD_INTERSECTION(WILDCARD, INTERSECTION),
    WILDCARD_NONE(WILDCARD, NONE),
    WILDCARD_NULL(WILDCARD, NULL),
    WILDCARD_PRIMITIVE(WILDCARD, PRIMITIVE),
    WILDCARD_TYPEVAR(WILDCARD, TYPEVAR),
    WILDCARD_UNION(WILDCARD, UNION),
    WILDCARD_WILDCARD(WILDCARD, WILDCARD);

    public final AtmKind type1Kind;
    public final AtmKind type2Kind;

    AtmCombo(final AtmKind type1Kind, AtmKind type2Kind) {
        this.type1Kind = type1Kind;
        this.type2Kind = type2Kind;
    }

    /**
     * Used to locate AtmCombo pairs using AtmKinds as indices into a two-dimensional array. This
     * ensures that all pairs are included.
     */
    private static final AtmCombo[][] comboMap =
            new AtmCombo[AtmKind.values().length][AtmKind.values().length];

    static {
        for (final AtmCombo atmCombo : AtmCombo.values()) {
            comboMap[atmCombo.type1Kind.ordinal()][atmCombo.type2Kind.ordinal()] = atmCombo;
        }
    }

    /**
     * @return the AtmCombo corresponding to the given ATM pair of the given ATMKinds. e.g.
     *     {@literal (AtmKind.NULL, AtmKind.EXECUTABLE) => AtmCombo.NULL_EXECUTABLE}
     */
    public static AtmCombo valueOf(final AtmKind type1, final AtmKind type2) {
        return comboMap[type1.ordinal()][type2.ordinal()];
    }

    /**
     * @return the AtmCombo corresponding to the pair of the classes for the given
     *     AnnotatedTypeMirrors. e.g. {@literal (AnnotatedPrimitiveType, AnnotatedDeclaredType) =>
     *     AtmCombo.PRIMITIVE_DECLARED}
     */
    public static AtmCombo valueOf(
            final AnnotatedTypeMirror type1, final AnnotatedTypeMirror type2) {
        return valueOf(AtmKind.valueOf(type1), AtmKind.valueOf(type2));
    }

    /**
     * Call the visit method that corresponds to the AtmCombo that represents the classes of type1
     * and type2. That is, get the combo for type1 and type 2, use it to identify the correct
     * visitor method, and call that method with type1, type2 and initial param as arguments to the
     * visit method.
     *
     * @param type1 first argument to the called visit method
     * @param type2 second argument to the called visit method
     * @param initialParam the parameter passed to the called visit method
     * @param visitor the visitor that is visiting the given types
     * @param <RETURN_TYPE> the return type of the visitor's visit methods
     * @param <PARAM> the parameter type of the visitor's visit methods
     * @return the return value of the visit method called
     */
    public static <RETURN_TYPE, PARAM> RETURN_TYPE accept(
            final AnnotatedTypeMirror type1,
            final AnnotatedTypeMirror type2,
            final PARAM initialParam,
            final AtmComboVisitor<RETURN_TYPE, PARAM> visitor) {
        final AtmCombo combo = valueOf(type1, type2);
        switch (combo) {
            case ARRAY_ARRAY:
                return visitor.visitArray_Array(
                        (AnnotatedArrayType) type1, (AnnotatedArrayType) type2, initialParam);

            case ARRAY_DECLARED:
                return visitor.visitArray_Declared(
                        (AnnotatedArrayType) type1, (AnnotatedDeclaredType) type2, initialParam);

            case ARRAY_EXECUTABLE:
                return visitor.visitArray_Executable(
                        (AnnotatedArrayType) type1, (AnnotatedExecutableType) type2, initialParam);

            case ARRAY_INTERSECTION:
                return visitor.visitArray_Intersection(
                        (AnnotatedArrayType) type1,
                        (AnnotatedIntersectionType) type2,
                        initialParam);

            case ARRAY_NONE:
                return visitor.visitArray_None(
                        (AnnotatedArrayType) type1, (AnnotatedNoType) type2, initialParam);

            case ARRAY_NULL:
                return visitor.visitArray_Null(
                        (AnnotatedArrayType) type1, (AnnotatedNullType) type2, initialParam);

            case ARRAY_PRIMITIVE:
                return visitor.visitArray_Primitive(
                        (AnnotatedArrayType) type1, (AnnotatedPrimitiveType) type2, initialParam);

            case ARRAY_TYPEVAR:
                return visitor.visitArray_Typevar(
                        (AnnotatedArrayType) type1, (AnnotatedTypeVariable) type2, initialParam);

            case ARRAY_UNION:
                return visitor.visitArray_Union(
                        (AnnotatedArrayType) type1, (AnnotatedUnionType) type2, initialParam);

            case ARRAY_WILDCARD:
                return visitor.visitArray_Wildcard(
                        (AnnotatedArrayType) type1, (AnnotatedWildcardType) type2, initialParam);

            case DECLARED_ARRAY:
                return visitor.visitDeclared_Array(
                        (AnnotatedDeclaredType) type1, (AnnotatedArrayType) type2, initialParam);

            case DECLARED_DECLARED:
                return visitor.visitDeclared_Declared(
                        (AnnotatedDeclaredType) type1, (AnnotatedDeclaredType) type2, initialParam);

            case DECLARED_EXECUTABLE:
                return visitor.visitDeclared_Executable(
                        (AnnotatedDeclaredType) type1,
                        (AnnotatedExecutableType) type2,
                        initialParam);

            case DECLARED_INTERSECTION:
                return visitor.visitDeclared_Intersection(
                        (AnnotatedDeclaredType) type1,
                        (AnnotatedIntersectionType) type2,
                        initialParam);

            case DECLARED_NONE:
                return visitor.visitDeclared_None(
                        (AnnotatedDeclaredType) type1, (AnnotatedNoType) type2, initialParam);

            case DECLARED_NULL:
                return visitor.visitDeclared_Null(
                        (AnnotatedDeclaredType) type1, (AnnotatedNullType) type2, initialParam);

            case DECLARED_PRIMITIVE:
                return visitor.visitDeclared_Primitive(
                        (AnnotatedDeclaredType) type1,
                        (AnnotatedPrimitiveType) type2,
                        initialParam);

            case DECLARED_TYPEVAR:
                return visitor.visitDeclared_Typevar(
                        (AnnotatedDeclaredType) type1, (AnnotatedTypeVariable) type2, initialParam);

            case DECLARED_UNION:
                return visitor.visitDeclared_Union(
                        (AnnotatedDeclaredType) type1, (AnnotatedUnionType) type2, initialParam);

            case DECLARED_WILDCARD:
                return visitor.visitDeclared_Wildcard(
                        (AnnotatedDeclaredType) type1, (AnnotatedWildcardType) type2, initialParam);

            case EXECUTABLE_ARRAY:
                return visitor.visitExecutable_Array(
                        (AnnotatedExecutableType) type1, (AnnotatedArrayType) type2, initialParam);

            case EXECUTABLE_DECLARED:
                return visitor.visitExecutable_Declared(
                        (AnnotatedExecutableType) type1,
                        (AnnotatedDeclaredType) type2,
                        initialParam);

            case EXECUTABLE_EXECUTABLE:
                return visitor.visitExecutable_Executable(
                        (AnnotatedExecutableType) type1,
                        (AnnotatedExecutableType) type2,
                        initialParam);

            case EXECUTABLE_INTERSECTION:
                return visitor.visitExecutable_Intersection(
                        (AnnotatedExecutableType) type1,
                        (AnnotatedIntersectionType) type2,
                        initialParam);

            case EXECUTABLE_NONE:
                return visitor.visitExecutable_None(
                        (AnnotatedExecutableType) type1, (AnnotatedNoType) type2, initialParam);

            case EXECUTABLE_NULL:
                return visitor.visitExecutable_Null(
                        (AnnotatedExecutableType) type1, (AnnotatedNullType) type2, initialParam);

            case EXECUTABLE_PRIMITIVE:
                return visitor.visitExecutable_Primitive(
                        (AnnotatedExecutableType) type1,
                        (AnnotatedPrimitiveType) type2,
                        initialParam);

            case EXECUTABLE_TYPEVAR:
                return visitor.visitExecutable_Typevar(
                        (AnnotatedExecutableType) type1,
                        (AnnotatedTypeVariable) type2,
                        initialParam);

            case EXECUTABLE_UNION:
                return visitor.visitExecutable_Union(
                        (AnnotatedExecutableType) type1, (AnnotatedUnionType) type2, initialParam);

            case EXECUTABLE_WILDCARD:
                return visitor.visitExecutable_Wildcard(
                        (AnnotatedExecutableType) type1,
                        (AnnotatedWildcardType) type2,
                        initialParam);

            case INTERSECTION_ARRAY:
                return visitor.visitIntersection_Array(
                        (AnnotatedIntersectionType) type1,
                        (AnnotatedArrayType) type2,
                        initialParam);

            case INTERSECTION_DECLARED:
                return visitor.visitIntersection_Declared(
                        (AnnotatedIntersectionType) type1,
                        (AnnotatedDeclaredType) type2,
                        initialParam);

            case INTERSECTION_EXECUTABLE:
                return visitor.visitIntersection_Executable(
                        (AnnotatedIntersectionType) type1,
                        (AnnotatedExecutableType) type2,
                        initialParam);

            case INTERSECTION_INTERSECTION:
                return visitor.visitIntersection_Intersection(
                        (AnnotatedIntersectionType) type1,
                        (AnnotatedIntersectionType) type2,
                        initialParam);

            case INTERSECTION_NONE:
                return visitor.visitIntersection_None(
                        (AnnotatedIntersectionType) type1, (AnnotatedNoType) type2, initialParam);

            case INTERSECTION_NULL:
                return visitor.visitIntersection_Null(
                        (AnnotatedIntersectionType) type1, (AnnotatedNullType) type2, initialParam);

            case INTERSECTION_PRIMITIVE:
                return visitor.visitIntersection_Primitive(
                        (AnnotatedIntersectionType) type1,
                        (AnnotatedPrimitiveType) type2,
                        initialParam);

            case INTERSECTION_TYPEVAR:
                return visitor.visitIntersection_Typevar(
                        (AnnotatedIntersectionType) type1,
                        (AnnotatedTypeVariable) type2,
                        initialParam);

            case INTERSECTION_UNION:
                return visitor.visitIntersection_Union(
                        (AnnotatedIntersectionType) type1,
                        (AnnotatedUnionType) type2,
                        initialParam);

            case INTERSECTION_WILDCARD:
                return visitor.visitIntersection_Wildcard(
                        (AnnotatedIntersectionType) type1,
                        (AnnotatedWildcardType) type2,
                        initialParam);

            case NONE_ARRAY:
                return visitor.visitNone_Array(
                        (AnnotatedNoType) type1, (AnnotatedArrayType) type2, initialParam);

            case NONE_DECLARED:
                return visitor.visitNone_Declared(
                        (AnnotatedNoType) type1, (AnnotatedDeclaredType) type2, initialParam);

            case NONE_EXECUTABLE:
                return visitor.visitNone_Executable(
                        (AnnotatedNoType) type1, (AnnotatedExecutableType) type2, initialParam);

            case NONE_INTERSECTION:
                return visitor.visitNone_Intersection(
                        (AnnotatedNoType) type1, (AnnotatedIntersectionType) type2, initialParam);

            case NONE_NONE:
                return visitor.visitNone_None(
                        (AnnotatedNoType) type1, (AnnotatedNoType) type2, initialParam);

            case NONE_NULL:
                return visitor.visitNone_Null(
                        (AnnotatedNoType) type1, (AnnotatedNullType) type2, initialParam);

            case NONE_PRIMITIVE:
                return visitor.visitNone_Primitive(
                        (AnnotatedNoType) type1, (AnnotatedPrimitiveType) type2, initialParam);

            case NONE_UNION:
                return visitor.visitNone_Union(
                        (AnnotatedNoType) type1, (AnnotatedUnionType) type2, initialParam);

            case NONE_WILDCARD:
                return visitor.visitNone_Wildcard(
                        (AnnotatedNoType) type1, (AnnotatedWildcardType) type2, initialParam);

            case NULL_ARRAY:
                return visitor.visitNull_Array(
                        (AnnotatedNullType) type1, (AnnotatedArrayType) type2, initialParam);

            case NULL_DECLARED:
                return visitor.visitNull_Declared(
                        (AnnotatedNullType) type1, (AnnotatedDeclaredType) type2, initialParam);

            case NULL_EXECUTABLE:
                return visitor.visitNull_Executable(
                        (AnnotatedNullType) type1, (AnnotatedExecutableType) type2, initialParam);

            case NULL_INTERSECTION:
                return visitor.visitNull_Intersection(
                        (AnnotatedNullType) type1, (AnnotatedIntersectionType) type2, initialParam);

            case NULL_NONE:
                return visitor.visitNull_None(
                        (AnnotatedNullType) type1, (AnnotatedNoType) type2, initialParam);

            case NULL_NULL:
                return visitor.visitNull_Null(
                        (AnnotatedNullType) type1, (AnnotatedNullType) type2, initialParam);

            case NULL_PRIMITIVE:
                return visitor.visitNull_Primitive(
                        (AnnotatedNullType) type1, (AnnotatedPrimitiveType) type2, initialParam);

            case NULL_TYPEVAR:
                return visitor.visitNull_Typevar(
                        (AnnotatedNullType) type1, (AnnotatedTypeVariable) type2, initialParam);

            case NULL_UNION:
                return visitor.visitNull_Union(
                        (AnnotatedNullType) type1, (AnnotatedUnionType) type2, initialParam);

            case NULL_WILDCARD:
                return visitor.visitNull_Wildcard(
                        (AnnotatedNullType) type1, (AnnotatedWildcardType) type2, initialParam);

            case PRIMITIVE_ARRAY:
                return visitor.visitPrimitive_Array(
                        (AnnotatedPrimitiveType) type1, (AnnotatedArrayType) type2, initialParam);

            case PRIMITIVE_DECLARED:
                return visitor.visitPrimitive_Declared(
                        (AnnotatedPrimitiveType) type1,
                        (AnnotatedDeclaredType) type2,
                        initialParam);

            case PRIMITIVE_EXECUTABLE:
                return visitor.visitPrimitive_Executable(
                        (AnnotatedPrimitiveType) type1,
                        (AnnotatedExecutableType) type2,
                        initialParam);

            case PRIMITIVE_INTERSECTION:
                return visitor.visitPrimitive_Intersection(
                        (AnnotatedPrimitiveType) type1,
                        (AnnotatedIntersectionType) type2,
                        initialParam);

            case PRIMITIVE_NONE:
                return visitor.visitPrimitive_None(
                        (AnnotatedPrimitiveType) type1, (AnnotatedNoType) type2, initialParam);

            case PRIMITIVE_NULL:
                return visitor.visitPrimitive_Null(
                        (AnnotatedPrimitiveType) type1, (AnnotatedNullType) type2, initialParam);

            case PRIMITIVE_PRIMITIVE:
                return visitor.visitPrimitive_Primitive(
                        (AnnotatedPrimitiveType) type1,
                        (AnnotatedPrimitiveType) type2,
                        initialParam);

            case PRIMITIVE_TYPEVAR:
                return visitor.visitPrimitive_Typevar(
                        (AnnotatedPrimitiveType) type1,
                        (AnnotatedTypeVariable) type2,
                        initialParam);

            case PRIMITIVE_UNION:
                return visitor.visitPrimitive_Union(
                        (AnnotatedPrimitiveType) type1, (AnnotatedUnionType) type2, initialParam);

            case PRIMITIVE_WILDCARD:
                return visitor.visitPrimitive_Wildcard(
                        (AnnotatedPrimitiveType) type1,
                        (AnnotatedWildcardType) type2,
                        initialParam);

            case UNION_ARRAY:
                return visitor.visitUnion_Array(
                        (AnnotatedUnionType) type1, (AnnotatedArrayType) type2, initialParam);

            case UNION_DECLARED:
                return visitor.visitUnion_Declared(
                        (AnnotatedUnionType) type1, (AnnotatedDeclaredType) type2, initialParam);

            case UNION_EXECUTABLE:
                return visitor.visitUnion_Executable(
                        (AnnotatedUnionType) type1, (AnnotatedExecutableType) type2, initialParam);

            case UNION_INTERSECTION:
                return visitor.visitUnion_Intersection(
                        (AnnotatedUnionType) type1,
                        (AnnotatedIntersectionType) type2,
                        initialParam);

            case UNION_NONE:
                return visitor.visitUnion_None(
                        (AnnotatedUnionType) type1, (AnnotatedNoType) type2, initialParam);

            case UNION_NULL:
                return visitor.visitUnion_Null(
                        (AnnotatedUnionType) type1, (AnnotatedNullType) type2, initialParam);

            case UNION_PRIMITIVE:
                return visitor.visitUnion_Primitive(
                        (AnnotatedUnionType) type1, (AnnotatedPrimitiveType) type2, initialParam);

            case UNION_TYPEVAR:
                return visitor.visitUnion_Typevar(
                        (AnnotatedUnionType) type1, (AnnotatedTypeVariable) type2, initialParam);

            case UNION_UNION:
                return visitor.visitUnion_Union(
                        (AnnotatedUnionType) type1, (AnnotatedUnionType) type2, initialParam);

            case UNION_WILDCARD:
                return visitor.visitUnion_Wildcard(
                        (AnnotatedUnionType) type1, (AnnotatedWildcardType) type2, initialParam);

            case TYPEVAR_ARRAY:
                return visitor.visitTypevar_Array(
                        (AnnotatedTypeVariable) type1, (AnnotatedArrayType) type2, initialParam);

            case TYPEVAR_DECLARED:
                return visitor.visitTypevar_Declared(
                        (AnnotatedTypeVariable) type1, (AnnotatedDeclaredType) type2, initialParam);

            case TYPEVAR_EXECUTABLE:
                return visitor.visitTypevar_Executable(
                        (AnnotatedTypeVariable) type1,
                        (AnnotatedExecutableType) type2,
                        initialParam);

            case TYPEVAR_INTERSECTION:
                return visitor.visitTypevar_Intersection(
                        (AnnotatedTypeVariable) type1,
                        (AnnotatedIntersectionType) type2,
                        initialParam);

            case TYPEVAR_NONE:
                return visitor.visitTypevar_None(
                        (AnnotatedTypeVariable) type1, (AnnotatedNoType) type2, initialParam);

            case TYPEVAR_NULL:
                return visitor.visitTypevar_Null(
                        (AnnotatedTypeVariable) type1, (AnnotatedNullType) type2, initialParam);

            case TYPEVAR_PRIMITIVE:
                return visitor.visitTypevar_Primitive(
                        (AnnotatedTypeVariable) type1,
                        (AnnotatedPrimitiveType) type2,
                        initialParam);

            case TYPEVAR_TYPEVAR:
                return visitor.visitTypevar_Typevar(
                        (AnnotatedTypeVariable) type1, (AnnotatedTypeVariable) type2, initialParam);

            case TYPEVAR_UNION:
                return visitor.visitTypevar_Union(
                        (AnnotatedTypeVariable) type1, (AnnotatedUnionType) type2, initialParam);

            case TYPEVAR_WILDCARD:
                return visitor.visitTypevar_Wildcard(
                        (AnnotatedTypeVariable) type1, (AnnotatedWildcardType) type2, initialParam);

            case WILDCARD_ARRAY:
                return visitor.visitWildcard_Array(
                        (AnnotatedWildcardType) type1, (AnnotatedArrayType) type2, initialParam);

            case WILDCARD_DECLARED:
                return visitor.visitWildcard_Declared(
                        (AnnotatedWildcardType) type1, (AnnotatedDeclaredType) type2, initialParam);

            case WILDCARD_EXECUTABLE:
                return visitor.visitWildcard_Executable(
                        (AnnotatedWildcardType) type1,
                        (AnnotatedExecutableType) type2,
                        initialParam);

            case WILDCARD_INTERSECTION:
                return visitor.visitWildcard_Intersection(
                        (AnnotatedWildcardType) type1,
                        (AnnotatedIntersectionType) type2,
                        initialParam);

            case WILDCARD_NONE:
                return visitor.visitWildcard_None(
                        (AnnotatedWildcardType) type1, (AnnotatedNoType) type2, initialParam);

            case WILDCARD_NULL:
                return visitor.visitWildcard_Null(
                        (AnnotatedWildcardType) type1, (AnnotatedNullType) type2, initialParam);

            case WILDCARD_PRIMITIVE:
                return visitor.visitWildcard_Primitive(
                        (AnnotatedWildcardType) type1,
                        (AnnotatedPrimitiveType) type2,
                        initialParam);

            case WILDCARD_TYPEVAR:
                return visitor.visitWildcard_Typevar(
                        (AnnotatedWildcardType) type1, (AnnotatedTypeVariable) type2, initialParam);

            case WILDCARD_UNION:
                return visitor.visitWildcard_Union(
                        (AnnotatedWildcardType) type1, (AnnotatedUnionType) type2, initialParam);

            case WILDCARD_WILDCARD:
                return visitor.visitWildcard_Wildcard(
                        (AnnotatedWildcardType) type1, (AnnotatedWildcardType) type2, initialParam);

            default:
                // Reaching this point indicates that there is an AtmCombo missing
                throw new BugInCF("Unhandled AtmCombo ( " + combo + " ) ");
        }
    }
}
