package org.checkerframework.framework.type.visitor;

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

/**
 * Visitor interface for all pair-wise combinations of AnnotatedTypeMirrors. See AtmCombo, it
 * enumerates all possible combinations and provides an "accept" method used to call AtmComboVisitor
 * visit methods.
 *
 * @param <RETURN_TYPE> the type returned by each visit method
 * @param <PARAM> the type of a single value that is passed to every visit method. It can act as
 *     global state.
 */
public interface AtmComboVisitor<RETURN_TYPE, PARAM> {

  public RETURN_TYPE visitArray_Array(
      AnnotatedArrayType subtype, AnnotatedArrayType supertype, PARAM param);

  public RETURN_TYPE visitArray_Declared(
      AnnotatedArrayType subtype, AnnotatedDeclaredType supertype, PARAM param);

  public RETURN_TYPE visitArray_Executable(
      AnnotatedArrayType subtype, AnnotatedExecutableType supertype, PARAM param);

  public RETURN_TYPE visitArray_Intersection(
      AnnotatedArrayType subtype, AnnotatedIntersectionType supertype, PARAM param);

  public RETURN_TYPE visitArray_None(
      AnnotatedArrayType subtype, AnnotatedNoType supertype, PARAM param);

  public RETURN_TYPE visitArray_Null(
      AnnotatedArrayType subtype, AnnotatedNullType supertype, PARAM param);

  public RETURN_TYPE visitArray_Primitive(
      AnnotatedArrayType subtype, AnnotatedPrimitiveType supertype, PARAM param);

  public RETURN_TYPE visitArray_Typevar(
      AnnotatedArrayType subtype, AnnotatedTypeVariable supertype, PARAM param);

  public RETURN_TYPE visitArray_Union(
      AnnotatedArrayType subtype, AnnotatedUnionType supertype, PARAM param);

  public RETURN_TYPE visitArray_Wildcard(
      AnnotatedArrayType subtype, AnnotatedWildcardType supertype, PARAM param);

  public RETURN_TYPE visitDeclared_Array(
      AnnotatedDeclaredType subtype, AnnotatedArrayType supertype, PARAM param);

  public RETURN_TYPE visitDeclared_Declared(
      AnnotatedDeclaredType subtype, AnnotatedDeclaredType supertype, PARAM param);

  public RETURN_TYPE visitDeclared_Executable(
      AnnotatedDeclaredType subtype, AnnotatedExecutableType supertype, PARAM param);

  public RETURN_TYPE visitDeclared_Intersection(
      AnnotatedDeclaredType subtype, AnnotatedIntersectionType supertype, PARAM param);

  public RETURN_TYPE visitDeclared_None(
      AnnotatedDeclaredType subtype, AnnotatedNoType supertype, PARAM param);

  public RETURN_TYPE visitDeclared_Null(
      AnnotatedDeclaredType subtype, AnnotatedNullType supertype, PARAM param);

  public RETURN_TYPE visitDeclared_Primitive(
      AnnotatedDeclaredType subtype, AnnotatedPrimitiveType supertype, PARAM param);

  public RETURN_TYPE visitDeclared_Typevar(
      AnnotatedDeclaredType subtype, AnnotatedTypeVariable supertype, PARAM param);

  public RETURN_TYPE visitDeclared_Union(
      AnnotatedDeclaredType subtype, AnnotatedUnionType supertype, PARAM param);

  public RETURN_TYPE visitDeclared_Wildcard(
      AnnotatedDeclaredType subtype, AnnotatedWildcardType supertype, PARAM param);

  public RETURN_TYPE visitExecutable_Array(
      AnnotatedExecutableType subtype, AnnotatedArrayType supertype, PARAM param);

  public RETURN_TYPE visitExecutable_Declared(
      AnnotatedExecutableType subtype, AnnotatedDeclaredType supertype, PARAM param);

  public RETURN_TYPE visitExecutable_Executable(
      AnnotatedExecutableType subtype, AnnotatedExecutableType supertype, PARAM param);

  public RETURN_TYPE visitExecutable_Intersection(
      AnnotatedExecutableType subtype, AnnotatedIntersectionType supertype, PARAM param);

  public RETURN_TYPE visitExecutable_None(
      AnnotatedExecutableType subtype, AnnotatedNoType supertype, PARAM param);

  public RETURN_TYPE visitExecutable_Null(
      AnnotatedExecutableType subtype, AnnotatedNullType supertype, PARAM param);

  public RETURN_TYPE visitExecutable_Primitive(
      AnnotatedExecutableType subtype, AnnotatedPrimitiveType supertype, PARAM param);

  public RETURN_TYPE visitExecutable_Typevar(
      AnnotatedExecutableType subtype, AnnotatedTypeVariable supertype, PARAM param);

  public RETURN_TYPE visitExecutable_Union(
      AnnotatedExecutableType subtype, AnnotatedUnionType supertype, PARAM param);

  public RETURN_TYPE visitExecutable_Wildcard(
      AnnotatedExecutableType subtype, AnnotatedWildcardType supertype, PARAM param);

  public RETURN_TYPE visitIntersection_Array(
      AnnotatedIntersectionType subtype, AnnotatedArrayType supertype, PARAM param);

  public RETURN_TYPE visitIntersection_Declared(
      AnnotatedIntersectionType subtype, AnnotatedDeclaredType supertype, PARAM param);

  public RETURN_TYPE visitIntersection_Executable(
      AnnotatedIntersectionType subtype, AnnotatedExecutableType supertype, PARAM param);

  public RETURN_TYPE visitIntersection_Intersection(
      AnnotatedIntersectionType subtype, AnnotatedIntersectionType supertype, PARAM param);

  public RETURN_TYPE visitIntersection_None(
      AnnotatedIntersectionType subtype, AnnotatedNoType supertype, PARAM param);

  public RETURN_TYPE visitIntersection_Null(
      AnnotatedIntersectionType subtype, AnnotatedNullType supertype, PARAM param);

  public RETURN_TYPE visitIntersection_Primitive(
      AnnotatedIntersectionType subtype, AnnotatedPrimitiveType supertype, PARAM param);

  public RETURN_TYPE visitIntersection_Typevar(
      AnnotatedIntersectionType subtype, AnnotatedTypeVariable supertype, PARAM param);

  public RETURN_TYPE visitIntersection_Union(
      AnnotatedIntersectionType subtype, AnnotatedUnionType supertype, PARAM param);

  public RETURN_TYPE visitIntersection_Wildcard(
      AnnotatedIntersectionType subtype, AnnotatedWildcardType supertype, PARAM param);

  public RETURN_TYPE visitNone_Array(
      AnnotatedNoType subtype, AnnotatedArrayType supertype, PARAM param);

  public RETURN_TYPE visitNone_Declared(
      AnnotatedNoType subtype, AnnotatedDeclaredType supertype, PARAM param);

  public RETURN_TYPE visitNone_Executable(
      AnnotatedNoType subtype, AnnotatedExecutableType supertype, PARAM param);

  public RETURN_TYPE visitNone_Intersection(
      AnnotatedNoType subtype, AnnotatedIntersectionType supertype, PARAM param);

  public RETURN_TYPE visitNone_None(
      AnnotatedNoType subtype, AnnotatedNoType supertype, PARAM param);

  public RETURN_TYPE visitNone_Null(
      AnnotatedNoType subtype, AnnotatedNullType supertype, PARAM param);

  public RETURN_TYPE visitNone_Primitive(
      AnnotatedNoType subtype, AnnotatedPrimitiveType supertype, PARAM param);

  public RETURN_TYPE visitNone_Union(
      AnnotatedNoType subtype, AnnotatedUnionType supertype, PARAM param);

  public RETURN_TYPE visitNone_Wildcard(
      AnnotatedNoType subtype, AnnotatedWildcardType supertype, PARAM param);

  public RETURN_TYPE visitNull_Array(
      AnnotatedNullType subtype, AnnotatedArrayType supertype, PARAM param);

  public RETURN_TYPE visitNull_Declared(
      AnnotatedNullType subtype, AnnotatedDeclaredType supertype, PARAM param);

  public RETURN_TYPE visitNull_Executable(
      AnnotatedNullType subtype, AnnotatedExecutableType supertype, PARAM param);

  public RETURN_TYPE visitNull_Intersection(
      AnnotatedNullType subtype, AnnotatedIntersectionType supertype, PARAM param);

  public RETURN_TYPE visitNull_None(
      AnnotatedNullType subtype, AnnotatedNoType supertype, PARAM param);

  public RETURN_TYPE visitNull_Null(
      AnnotatedNullType subtype, AnnotatedNullType supertype, PARAM param);

  public RETURN_TYPE visitNull_Primitive(
      AnnotatedNullType subtype, AnnotatedPrimitiveType supertype, PARAM param);

  public RETURN_TYPE visitNull_Typevar(
      AnnotatedNullType subtype, AnnotatedTypeVariable supertype, PARAM param);

  public RETURN_TYPE visitNull_Union(
      AnnotatedNullType subtype, AnnotatedUnionType supertype, PARAM param);

  public RETURN_TYPE visitNull_Wildcard(
      AnnotatedNullType subtype, AnnotatedWildcardType supertype, PARAM param);

  public RETURN_TYPE visitPrimitive_Array(
      AnnotatedPrimitiveType subtype, AnnotatedArrayType supertype, PARAM param);

  public RETURN_TYPE visitPrimitive_Declared(
      AnnotatedPrimitiveType subtype, AnnotatedDeclaredType supertype, PARAM param);

  public RETURN_TYPE visitPrimitive_Executable(
      AnnotatedPrimitiveType subtype, AnnotatedExecutableType supertype, PARAM param);

  public RETURN_TYPE visitPrimitive_Intersection(
      AnnotatedPrimitiveType subtype, AnnotatedIntersectionType supertype, PARAM param);

  public RETURN_TYPE visitPrimitive_None(
      AnnotatedPrimitiveType subtype, AnnotatedNoType supertype, PARAM param);

  public RETURN_TYPE visitPrimitive_Null(
      AnnotatedPrimitiveType subtype, AnnotatedNullType supertype, PARAM param);

  public RETURN_TYPE visitPrimitive_Primitive(
      AnnotatedPrimitiveType subtype, AnnotatedPrimitiveType supertype, PARAM param);

  public RETURN_TYPE visitPrimitive_Typevar(
      AnnotatedPrimitiveType subtype, AnnotatedTypeVariable supertype, PARAM param);

  public RETURN_TYPE visitPrimitive_Union(
      AnnotatedPrimitiveType subtype, AnnotatedUnionType supertype, PARAM param);

  public RETURN_TYPE visitPrimitive_Wildcard(
      AnnotatedPrimitiveType subtype, AnnotatedWildcardType supertype, PARAM param);

  public RETURN_TYPE visitUnion_Array(
      AnnotatedUnionType subtype, AnnotatedArrayType supertype, PARAM param);

  public RETURN_TYPE visitUnion_Declared(
      AnnotatedUnionType subtype, AnnotatedDeclaredType supertype, PARAM param);

  public RETURN_TYPE visitUnion_Executable(
      AnnotatedUnionType subtype, AnnotatedExecutableType supertype, PARAM param);

  public RETURN_TYPE visitUnion_Intersection(
      AnnotatedUnionType subtype, AnnotatedIntersectionType supertype, PARAM param);

  public RETURN_TYPE visitUnion_None(
      AnnotatedUnionType subtype, AnnotatedNoType supertype, PARAM param);

  public RETURN_TYPE visitUnion_Null(
      AnnotatedUnionType subtype, AnnotatedNullType supertype, PARAM param);

  public RETURN_TYPE visitUnion_Primitive(
      AnnotatedUnionType subtype, AnnotatedPrimitiveType supertype, PARAM param);

  public RETURN_TYPE visitUnion_Typevar(
      AnnotatedUnionType subtype, AnnotatedTypeVariable supertype, PARAM param);

  public RETURN_TYPE visitUnion_Union(
      AnnotatedUnionType subtype, AnnotatedUnionType supertype, PARAM param);

  public RETURN_TYPE visitUnion_Wildcard(
      AnnotatedUnionType subtype, AnnotatedWildcardType supertype, PARAM param);

  public RETURN_TYPE visitTypevar_Array(
      AnnotatedTypeVariable subtype, AnnotatedArrayType supertype, PARAM param);

  public RETURN_TYPE visitTypevar_Declared(
      AnnotatedTypeVariable subtype, AnnotatedDeclaredType supertype, PARAM param);

  public RETURN_TYPE visitTypevar_Executable(
      AnnotatedTypeVariable subtype, AnnotatedExecutableType supertype, PARAM param);

  public RETURN_TYPE visitTypevar_Intersection(
      AnnotatedTypeVariable subtype, AnnotatedIntersectionType supertype, PARAM param);

  public RETURN_TYPE visitTypevar_None(
      AnnotatedTypeVariable subtype, AnnotatedNoType supertype, PARAM param);

  public RETURN_TYPE visitTypevar_Null(
      AnnotatedTypeVariable subtype, AnnotatedNullType supertype, PARAM param);

  public RETURN_TYPE visitTypevar_Primitive(
      AnnotatedTypeVariable subtype, AnnotatedPrimitiveType supertype, PARAM param);

  public RETURN_TYPE visitTypevar_Typevar(
      AnnotatedTypeVariable subtype, AnnotatedTypeVariable supertype, PARAM param);

  public RETURN_TYPE visitTypevar_Union(
      AnnotatedTypeVariable subtype, AnnotatedUnionType supertype, PARAM param);

  public RETURN_TYPE visitTypevar_Wildcard(
      AnnotatedTypeVariable subtype, AnnotatedWildcardType supertype, PARAM param);

  public RETURN_TYPE visitWildcard_Array(
      AnnotatedWildcardType subtype, AnnotatedArrayType supertype, PARAM param);

  public RETURN_TYPE visitWildcard_Declared(
      AnnotatedWildcardType subtype, AnnotatedDeclaredType supertype, PARAM param);

  public RETURN_TYPE visitWildcard_Executable(
      AnnotatedWildcardType subtype, AnnotatedExecutableType supertype, PARAM param);

  public RETURN_TYPE visitWildcard_Intersection(
      AnnotatedWildcardType subtype, AnnotatedIntersectionType supertype, PARAM param);

  public RETURN_TYPE visitWildcard_None(
      AnnotatedWildcardType subtype, AnnotatedNoType supertype, PARAM param);

  public RETURN_TYPE visitWildcard_Null(
      AnnotatedWildcardType subtype, AnnotatedNullType supertype, PARAM param);

  public RETURN_TYPE visitWildcard_Primitive(
      AnnotatedWildcardType subtype, AnnotatedPrimitiveType supertype, PARAM param);

  public RETURN_TYPE visitWildcard_Typevar(
      AnnotatedWildcardType subtype, AnnotatedTypeVariable supertype, PARAM param);

  public RETURN_TYPE visitWildcard_Union(
      AnnotatedWildcardType subtype, AnnotatedUnionType supertype, PARAM param);

  public RETURN_TYPE visitWildcard_Wildcard(
      AnnotatedWildcardType subtype, AnnotatedWildcardType supertype, PARAM param);
}
