package org.checkerframework.framework.type.visitor;

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
import org.checkerframework.framework.util.AtmCombo;
import org.checkerframework.javacutil.BugInCF;

/**
 * Implements all methods from AtmComboVisitor. By default all methods throw an exception. Implement
 * only those methods you expect to be called on your subclass.
 *
 * <p>This class does no traversal.
 */
public abstract class AbstractAtmComboVisitor<RETURN_TYPE, PARAM>
        implements AtmComboVisitor<RETURN_TYPE, PARAM> {

    /**
     * Formats type1, type2 and param into an error message used by all methods of
     * AbstractAtmComboVisitor that are not overridden. Normally, this method should indicate that
     * the given method (and therefore the given pair of type mirror classes) is not supported by
     * this class.
     *
     * @param type1 the first AnnotatedTypeMirror parameter to the visit method called
     * @param type2 the second AnnotatedTypeMirror parameter to the visit method called
     * @param param subtype specific parameter passed to every visit method
     * @return an error message
     */
    protected abstract String defaultErrorMessage(
            AnnotatedTypeMirror type1, AnnotatedTypeMirror type2, PARAM param);

    /**
     * Called by the default implementation of every AbstractAtmComboVisitor visit method. This
     * methodnS issues a runtime exception by default. In general, it should handle the case where a
     * visit method has been called with a pair of type mirrors that should never be passed to this
     * particular visitor.
     *
     * @param type1 the first AnnotatedTypeMirror parameter to the visit method called
     * @param type2 the second AnnotatedTypeMirror parameter to the visit method called
     * @param param subtype specific parameter passed to every visit method
     */
    protected RETURN_TYPE defaultAction(
            AnnotatedTypeMirror type1, AnnotatedTypeMirror type2, PARAM param) {
        throw new BugInCF(defaultErrorMessage(type1, type2, param));
    }

    /**
     * Dispatches to a more specific {@code visit*} method.
     *
     * @param type1 the first type to visit
     * @param type2 the second type to visit
     * @param param a value passed to every visit method
     * @return the result of calling the more specific {@code visit*} method
     */
    public RETURN_TYPE visit(
            final AnnotatedTypeMirror type1, final AnnotatedTypeMirror type2, PARAM param) {
        return AtmCombo.accept(type1, type2, param, this);
    }

    @Override
    public RETURN_TYPE visitArray_Array(
            AnnotatedArrayType type1, AnnotatedArrayType type2, PARAM param) {
        return defaultAction(type1, type2, param);
    }

    @Override
    public RETURN_TYPE visitArray_Declared(
            AnnotatedArrayType type1, AnnotatedDeclaredType type2, PARAM param) {
        return defaultAction(type1, type2, param);
    }

    @Override
    public RETURN_TYPE visitArray_Executable(
            AnnotatedArrayType type1, AnnotatedExecutableType type2, PARAM param) {
        return defaultAction(type1, type2, param);
    }

    @Override
    public RETURN_TYPE visitArray_Intersection(
            AnnotatedArrayType type1, AnnotatedIntersectionType type2, PARAM param) {
        return defaultAction(type1, type2, param);
    }

    @Override
    public RETURN_TYPE visitArray_None(
            AnnotatedArrayType type1, AnnotatedNoType type2, PARAM param) {
        return defaultAction(type1, type2, param);
    }

    @Override
    public RETURN_TYPE visitArray_Null(
            AnnotatedArrayType type1, AnnotatedNullType type2, PARAM param) {
        return defaultAction(type1, type2, param);
    }

    @Override
    public RETURN_TYPE visitArray_Primitive(
            AnnotatedArrayType type1, AnnotatedPrimitiveType type2, PARAM param) {
        return defaultAction(type1, type2, param);
    }

    @Override
    public RETURN_TYPE visitArray_Typevar(
            AnnotatedArrayType type1, AnnotatedTypeVariable type2, PARAM param) {
        return defaultAction(type1, type2, param);
    }

    @Override
    public RETURN_TYPE visitArray_Union(
            AnnotatedArrayType type1, AnnotatedUnionType type2, PARAM param) {
        return defaultAction(type1, type2, param);
    }

    @Override
    public RETURN_TYPE visitArray_Wildcard(
            AnnotatedArrayType type1, AnnotatedWildcardType type2, PARAM param) {
        return defaultAction(type1, type2, param);
    }

    @Override
    public RETURN_TYPE visitDeclared_Array(
            AnnotatedDeclaredType type1, AnnotatedArrayType type2, PARAM param) {
        return defaultAction(type1, type2, param);
    }

    @Override
    public RETURN_TYPE visitDeclared_Declared(
            AnnotatedDeclaredType type1, AnnotatedDeclaredType type2, PARAM param) {
        return defaultAction(type1, type2, param);
    }

    @Override
    public RETURN_TYPE visitDeclared_Executable(
            AnnotatedDeclaredType type1, AnnotatedExecutableType type2, PARAM param) {
        return defaultAction(type1, type2, param);
    }

    @Override
    public RETURN_TYPE visitDeclared_Intersection(
            AnnotatedDeclaredType type1, AnnotatedIntersectionType type2, PARAM param) {
        return defaultAction(type1, type2, param);
    }

    @Override
    public RETURN_TYPE visitDeclared_None(
            AnnotatedDeclaredType type1, AnnotatedNoType type2, PARAM param) {
        return defaultAction(type1, type2, param);
    }

    @Override
    public RETURN_TYPE visitDeclared_Null(
            AnnotatedDeclaredType type1, AnnotatedNullType type2, PARAM param) {
        return defaultAction(type1, type2, param);
    }

    @Override
    public RETURN_TYPE visitDeclared_Primitive(
            AnnotatedDeclaredType type1, AnnotatedPrimitiveType type2, PARAM param) {
        return defaultAction(type1, type2, param);
    }

    @Override
    public RETURN_TYPE visitDeclared_Typevar(
            AnnotatedDeclaredType type1, AnnotatedTypeVariable type2, PARAM param) {
        return defaultAction(type1, type2, param);
    }

    @Override
    public RETURN_TYPE visitDeclared_Union(
            AnnotatedDeclaredType type1, AnnotatedUnionType type2, PARAM param) {
        return defaultAction(type1, type2, param);
    }

    @Override
    public RETURN_TYPE visitDeclared_Wildcard(
            AnnotatedDeclaredType type1, AnnotatedWildcardType type2, PARAM param) {
        return defaultAction(type1, type2, param);
    }

    @Override
    public RETURN_TYPE visitExecutable_Array(
            AnnotatedExecutableType type1, AnnotatedArrayType type2, PARAM param) {
        return defaultAction(type1, type2, param);
    }

    @Override
    public RETURN_TYPE visitExecutable_Declared(
            AnnotatedExecutableType type1, AnnotatedDeclaredType type2, PARAM param) {
        return defaultAction(type1, type2, param);
    }

    @Override
    public RETURN_TYPE visitExecutable_Executable(
            AnnotatedExecutableType type1, AnnotatedExecutableType type2, PARAM param) {
        return defaultAction(type1, type2, param);
    }

    @Override
    public RETURN_TYPE visitExecutable_Intersection(
            AnnotatedExecutableType type1, AnnotatedIntersectionType type2, PARAM param) {
        return defaultAction(type1, type2, param);
    }

    @Override
    public RETURN_TYPE visitExecutable_None(
            AnnotatedExecutableType type1, AnnotatedNoType type2, PARAM param) {
        return defaultAction(type1, type2, param);
    }

    @Override
    public RETURN_TYPE visitExecutable_Null(
            AnnotatedExecutableType type1, AnnotatedNullType type2, PARAM param) {
        return defaultAction(type1, type2, param);
    }

    @Override
    public RETURN_TYPE visitExecutable_Primitive(
            AnnotatedExecutableType type1, AnnotatedPrimitiveType type2, PARAM param) {
        return defaultAction(type1, type2, param);
    }

    @Override
    public RETURN_TYPE visitExecutable_Typevar(
            AnnotatedExecutableType type1, AnnotatedTypeVariable type2, PARAM param) {
        return defaultAction(type1, type2, param);
    }

    @Override
    public RETURN_TYPE visitExecutable_Union(
            AnnotatedExecutableType type1, AnnotatedUnionType type2, PARAM param) {
        return defaultAction(type1, type2, param);
    }

    @Override
    public RETURN_TYPE visitExecutable_Wildcard(
            AnnotatedExecutableType type1, AnnotatedWildcardType type2, PARAM param) {
        return defaultAction(type1, type2, param);
    }

    @Override
    public RETURN_TYPE visitIntersection_Array(
            AnnotatedIntersectionType type1, AnnotatedArrayType type2, PARAM param) {
        return defaultAction(type1, type2, param);
    }

    @Override
    public RETURN_TYPE visitIntersection_Declared(
            AnnotatedIntersectionType type1, AnnotatedDeclaredType type2, PARAM param) {
        return defaultAction(type1, type2, param);
    }

    @Override
    public RETURN_TYPE visitIntersection_Executable(
            AnnotatedIntersectionType type1, AnnotatedExecutableType type2, PARAM param) {
        return defaultAction(type1, type2, param);
    }

    @Override
    public RETURN_TYPE visitIntersection_Intersection(
            AnnotatedIntersectionType type1, AnnotatedIntersectionType type2, PARAM param) {
        return defaultAction(type1, type2, param);
    }

    @Override
    public RETURN_TYPE visitIntersection_None(
            AnnotatedIntersectionType type1, AnnotatedNoType type2, PARAM param) {
        return defaultAction(type1, type2, param);
    }

    @Override
    public RETURN_TYPE visitIntersection_Null(
            AnnotatedIntersectionType type1, AnnotatedNullType type2, PARAM param) {
        return defaultAction(type1, type2, param);
    }

    @Override
    public RETURN_TYPE visitIntersection_Primitive(
            AnnotatedIntersectionType type1, AnnotatedPrimitiveType type2, PARAM param) {
        return defaultAction(type1, type2, param);
    }

    @Override
    public RETURN_TYPE visitIntersection_Typevar(
            AnnotatedIntersectionType type1, AnnotatedTypeVariable type2, PARAM param) {
        return defaultAction(type1, type2, param);
    }

    @Override
    public RETURN_TYPE visitIntersection_Union(
            AnnotatedIntersectionType type1, AnnotatedUnionType type2, PARAM param) {
        return defaultAction(type1, type2, param);
    }

    @Override
    public RETURN_TYPE visitIntersection_Wildcard(
            AnnotatedIntersectionType type1, AnnotatedWildcardType type2, PARAM param) {
        return defaultAction(type1, type2, param);
    }

    @Override
    public RETURN_TYPE visitNone_Array(
            AnnotatedNoType type1, AnnotatedArrayType type2, PARAM param) {
        return defaultAction(type1, type2, param);
    }

    @Override
    public RETURN_TYPE visitNone_Declared(
            AnnotatedNoType type1, AnnotatedDeclaredType type2, PARAM param) {
        return defaultAction(type1, type2, param);
    }

    @Override
    public RETURN_TYPE visitNone_Executable(
            AnnotatedNoType type1, AnnotatedExecutableType type2, PARAM param) {
        return defaultAction(type1, type2, param);
    }

    @Override
    public RETURN_TYPE visitNone_Intersection(
            AnnotatedNoType type1, AnnotatedIntersectionType type2, PARAM param) {
        return defaultAction(type1, type2, param);
    }

    @Override
    public RETURN_TYPE visitNone_None(AnnotatedNoType type1, AnnotatedNoType type2, PARAM param) {
        return defaultAction(type1, type2, param);
    }

    @Override
    public RETURN_TYPE visitNone_Null(AnnotatedNoType type1, AnnotatedNullType type2, PARAM param) {
        return defaultAction(type1, type2, param);
    }

    @Override
    public RETURN_TYPE visitNone_Primitive(
            AnnotatedNoType type1, AnnotatedPrimitiveType type2, PARAM param) {
        return defaultAction(type1, type2, param);
    }

    @Override
    public RETURN_TYPE visitNone_Union(
            AnnotatedNoType type1, AnnotatedUnionType type2, PARAM param) {
        return defaultAction(type1, type2, param);
    }

    @Override
    public RETURN_TYPE visitNone_Wildcard(
            AnnotatedNoType type1, AnnotatedWildcardType type2, PARAM param) {
        return defaultAction(type1, type2, param);
    }

    @Override
    public RETURN_TYPE visitNull_Array(
            AnnotatedNullType type1, AnnotatedArrayType type2, PARAM param) {
        return defaultAction(type1, type2, param);
    }

    @Override
    public RETURN_TYPE visitNull_Declared(
            AnnotatedNullType type1, AnnotatedDeclaredType type2, PARAM param) {
        return defaultAction(type1, type2, param);
    }

    @Override
    public RETURN_TYPE visitNull_Executable(
            AnnotatedNullType type1, AnnotatedExecutableType type2, PARAM param) {
        return defaultAction(type1, type2, param);
    }

    @Override
    public RETURN_TYPE visitNull_Intersection(
            AnnotatedNullType type1, AnnotatedIntersectionType type2, PARAM param) {
        return defaultAction(type1, type2, param);
    }

    @Override
    public RETURN_TYPE visitNull_None(AnnotatedNullType type1, AnnotatedNoType type2, PARAM param) {
        return defaultAction(type1, type2, param);
    }

    @Override
    public RETURN_TYPE visitNull_Null(
            AnnotatedNullType type1, AnnotatedNullType type2, PARAM param) {
        return defaultAction(type1, type2, param);
    }

    @Override
    public RETURN_TYPE visitNull_Primitive(
            AnnotatedNullType type1, AnnotatedPrimitiveType type2, PARAM param) {
        return defaultAction(type1, type2, param);
    }

    @Override
    public RETURN_TYPE visitNull_Typevar(
            AnnotatedNullType type1, AnnotatedTypeVariable type2, PARAM param) {
        return defaultAction(type1, type2, param);
    }

    @Override
    public RETURN_TYPE visitNull_Union(
            AnnotatedNullType type1, AnnotatedUnionType type2, PARAM param) {
        return defaultAction(type1, type2, param);
    }

    @Override
    public RETURN_TYPE visitNull_Wildcard(
            AnnotatedNullType type1, AnnotatedWildcardType type2, PARAM param) {
        return defaultAction(type1, type2, param);
    }

    @Override
    public RETURN_TYPE visitPrimitive_Array(
            AnnotatedPrimitiveType type1, AnnotatedArrayType type2, PARAM param) {
        return defaultAction(type1, type2, param);
    }

    @Override
    public RETURN_TYPE visitPrimitive_Declared(
            AnnotatedPrimitiveType type1, AnnotatedDeclaredType type2, PARAM param) {
        return defaultAction(type1, type2, param);
    }

    @Override
    public RETURN_TYPE visitPrimitive_Executable(
            AnnotatedPrimitiveType type1, AnnotatedExecutableType type2, PARAM param) {
        return defaultAction(type1, type2, param);
    }

    @Override
    public RETURN_TYPE visitPrimitive_Intersection(
            AnnotatedPrimitiveType type1, AnnotatedIntersectionType type2, PARAM param) {
        return defaultAction(type1, type2, param);
    }

    @Override
    public RETURN_TYPE visitPrimitive_None(
            AnnotatedPrimitiveType type1, AnnotatedNoType type2, PARAM param) {
        return defaultAction(type1, type2, param);
    }

    @Override
    public RETURN_TYPE visitPrimitive_Null(
            AnnotatedPrimitiveType type1, AnnotatedNullType type2, PARAM param) {
        return defaultAction(type1, type2, param);
    }

    @Override
    public RETURN_TYPE visitPrimitive_Primitive(
            AnnotatedPrimitiveType type1, AnnotatedPrimitiveType type2, PARAM param) {
        return defaultAction(type1, type2, param);
    }

    @Override
    public RETURN_TYPE visitPrimitive_Typevar(
            AnnotatedPrimitiveType type1, AnnotatedTypeVariable type2, PARAM param) {
        return defaultAction(type1, type2, param);
    }

    @Override
    public RETURN_TYPE visitPrimitive_Union(
            AnnotatedPrimitiveType type1, AnnotatedUnionType type2, PARAM param) {
        return defaultAction(type1, type2, param);
    }

    @Override
    public RETURN_TYPE visitPrimitive_Wildcard(
            AnnotatedPrimitiveType type1, AnnotatedWildcardType type2, PARAM param) {
        return defaultAction(type1, type2, param);
    }

    @Override
    public RETURN_TYPE visitUnion_Array(
            AnnotatedUnionType type1, AnnotatedArrayType type2, PARAM param) {
        return defaultAction(type1, type2, param);
    }

    @Override
    public RETURN_TYPE visitUnion_Declared(
            AnnotatedUnionType type1, AnnotatedDeclaredType type2, PARAM param) {
        return defaultAction(type1, type2, param);
    }

    @Override
    public RETURN_TYPE visitUnion_Executable(
            AnnotatedUnionType type1, AnnotatedExecutableType type2, PARAM param) {
        return defaultAction(type1, type2, param);
    }

    @Override
    public RETURN_TYPE visitUnion_Intersection(
            AnnotatedUnionType type1, AnnotatedIntersectionType type2, PARAM param) {
        return defaultAction(type1, type2, param);
    }

    @Override
    public RETURN_TYPE visitUnion_None(
            AnnotatedUnionType type1, AnnotatedNoType type2, PARAM param) {
        return defaultAction(type1, type2, param);
    }

    @Override
    public RETURN_TYPE visitUnion_Null(
            AnnotatedUnionType type1, AnnotatedNullType type2, PARAM param) {
        return defaultAction(type1, type2, param);
    }

    @Override
    public RETURN_TYPE visitUnion_Primitive(
            AnnotatedUnionType type1, AnnotatedPrimitiveType type2, PARAM param) {
        return defaultAction(type1, type2, param);
    }

    @Override
    public RETURN_TYPE visitUnion_Typevar(
            AnnotatedUnionType type1, AnnotatedTypeVariable type2, PARAM param) {
        return defaultAction(type1, type2, param);
    }

    @Override
    public RETURN_TYPE visitUnion_Union(
            AnnotatedUnionType type1, AnnotatedUnionType type2, PARAM param) {
        return defaultAction(type1, type2, param);
    }

    @Override
    public RETURN_TYPE visitUnion_Wildcard(
            AnnotatedUnionType type1, AnnotatedWildcardType type2, PARAM param) {
        return defaultAction(type1, type2, param);
    }

    @Override
    public RETURN_TYPE visitTypevar_Array(
            AnnotatedTypeVariable type1, AnnotatedArrayType type2, PARAM param) {
        return defaultAction(type1, type2, param);
    }

    @Override
    public RETURN_TYPE visitTypevar_Declared(
            AnnotatedTypeVariable type1, AnnotatedDeclaredType type2, PARAM param) {
        return defaultAction(type1, type2, param);
    }

    @Override
    public RETURN_TYPE visitTypevar_Executable(
            AnnotatedTypeVariable type1, AnnotatedExecutableType type2, PARAM param) {
        return defaultAction(type1, type2, param);
    }

    @Override
    public RETURN_TYPE visitTypevar_Intersection(
            AnnotatedTypeVariable type1, AnnotatedIntersectionType type2, PARAM param) {
        return defaultAction(type1, type2, param);
    }

    @Override
    public RETURN_TYPE visitTypevar_None(
            AnnotatedTypeVariable type1, AnnotatedNoType type2, PARAM param) {
        return defaultAction(type1, type2, param);
    }

    @Override
    public RETURN_TYPE visitTypevar_Null(
            AnnotatedTypeVariable type1, AnnotatedNullType type2, PARAM param) {
        return defaultAction(type1, type2, param);
    }

    @Override
    public RETURN_TYPE visitTypevar_Primitive(
            AnnotatedTypeVariable type1, AnnotatedPrimitiveType type2, PARAM param) {
        return defaultAction(type1, type2, param);
    }

    @Override
    public RETURN_TYPE visitTypevar_Typevar(
            AnnotatedTypeVariable type1, AnnotatedTypeVariable type2, PARAM param) {
        return defaultAction(type1, type2, param);
    }

    @Override
    public RETURN_TYPE visitTypevar_Union(
            AnnotatedTypeVariable type1, AnnotatedUnionType type2, PARAM param) {
        return defaultAction(type1, type2, param);
    }

    @Override
    public RETURN_TYPE visitTypevar_Wildcard(
            AnnotatedTypeVariable type1, AnnotatedWildcardType type2, PARAM param) {
        return defaultAction(type1, type2, param);
    }

    @Override
    public RETURN_TYPE visitWildcard_Array(
            AnnotatedWildcardType type1, AnnotatedArrayType type2, PARAM param) {
        return defaultAction(type1, type2, param);
    }

    @Override
    public RETURN_TYPE visitWildcard_Declared(
            AnnotatedWildcardType type1, AnnotatedDeclaredType type2, PARAM param) {
        return defaultAction(type1, type2, param);
    }

    @Override
    public RETURN_TYPE visitWildcard_Executable(
            AnnotatedWildcardType type1, AnnotatedExecutableType type2, PARAM param) {
        return defaultAction(type1, type2, param);
    }

    @Override
    public RETURN_TYPE visitWildcard_Intersection(
            AnnotatedWildcardType type1, AnnotatedIntersectionType type2, PARAM param) {
        return defaultAction(type1, type2, param);
    }

    @Override
    public RETURN_TYPE visitWildcard_None(
            AnnotatedWildcardType type1, AnnotatedNoType type2, PARAM param) {
        return defaultAction(type1, type2, param);
    }

    @Override
    public RETURN_TYPE visitWildcard_Null(
            AnnotatedWildcardType type1, AnnotatedNullType type2, PARAM param) {
        return defaultAction(type1, type2, param);
    }

    @Override
    public RETURN_TYPE visitWildcard_Primitive(
            AnnotatedWildcardType type1, AnnotatedPrimitiveType type2, PARAM param) {
        return defaultAction(type1, type2, param);
    }

    @Override
    public RETURN_TYPE visitWildcard_Typevar(
            AnnotatedWildcardType type1, AnnotatedTypeVariable type2, PARAM param) {
        return defaultAction(type1, type2, param);
    }

    @Override
    public RETURN_TYPE visitWildcard_Union(
            AnnotatedWildcardType type1, AnnotatedUnionType type2, PARAM param) {
        return defaultAction(type1, type2, param);
    }

    @Override
    public RETURN_TYPE visitWildcard_Wildcard(
            AnnotatedWildcardType type1, AnnotatedWildcardType type2, PARAM param) {
        return defaultAction(type1, type2, param);
    }
}
