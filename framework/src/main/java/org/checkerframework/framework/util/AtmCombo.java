package org.checkerframework.framework.util;

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

  /**
   * Returns the AtmKind corresponding to the class of atm.
   *
   * @return the AtmKind corresponding to the class of atm
   */
  public static AtmKind valueOf(final AnnotatedTypeMirror atm) {
    final Class<?> argClass = atm.getClass();

    for (AtmKind atmKind : AtmKind.values()) {
      final Class<?> kindClass = atmKind.atmClass;
      if (argClass == kindClass) {
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
  ARRAY_ARRAY(AtmKind.ARRAY, AtmKind.ARRAY),
  ARRAY_DECLARED(AtmKind.ARRAY, AtmKind.DECLARED),
  ARRAY_EXECUTABLE(AtmKind.ARRAY, AtmKind.EXECUTABLE),
  ARRAY_INTERSECTION(AtmKind.ARRAY, AtmKind.INTERSECTION),
  ARRAY_NONE(AtmKind.ARRAY, AtmKind.NONE),
  ARRAY_NULL(AtmKind.ARRAY, AtmKind.NULL),
  ARRAY_PRIMITIVE(AtmKind.ARRAY, AtmKind.PRIMITIVE),
  ARRAY_UNION(AtmKind.ARRAY, AtmKind.UNION),
  ARRAY_TYPEVAR(AtmKind.ARRAY, AtmKind.TYPEVAR),
  ARRAY_WILDCARD(AtmKind.ARRAY, AtmKind.WILDCARD),

  DECLARED_ARRAY(AtmKind.DECLARED, AtmKind.ARRAY),
  DECLARED_DECLARED(AtmKind.DECLARED, AtmKind.DECLARED),
  DECLARED_EXECUTABLE(AtmKind.DECLARED, AtmKind.EXECUTABLE),
  DECLARED_INTERSECTION(AtmKind.DECLARED, AtmKind.INTERSECTION),
  DECLARED_NONE(AtmKind.DECLARED, AtmKind.NONE),
  DECLARED_NULL(AtmKind.DECLARED, AtmKind.NULL),
  DECLARED_PRIMITIVE(AtmKind.DECLARED, AtmKind.PRIMITIVE),
  DECLARED_TYPEVAR(AtmKind.DECLARED, AtmKind.TYPEVAR),
  DECLARED_UNION(AtmKind.DECLARED, AtmKind.UNION),
  DECLARED_WILDCARD(AtmKind.DECLARED, AtmKind.WILDCARD),

  EXECUTABLE_ARRAY(AtmKind.EXECUTABLE, AtmKind.ARRAY),
  EXECUTABLE_DECLARED(AtmKind.EXECUTABLE, AtmKind.DECLARED),
  EXECUTABLE_EXECUTABLE(AtmKind.EXECUTABLE, AtmKind.EXECUTABLE),
  EXECUTABLE_INTERSECTION(AtmKind.EXECUTABLE, AtmKind.INTERSECTION),
  EXECUTABLE_NONE(AtmKind.EXECUTABLE, AtmKind.NONE),
  EXECUTABLE_NULL(AtmKind.EXECUTABLE, AtmKind.NULL),
  EXECUTABLE_PRIMITIVE(AtmKind.EXECUTABLE, AtmKind.PRIMITIVE),
  EXECUTABLE_TYPEVAR(AtmKind.EXECUTABLE, AtmKind.TYPEVAR),
  EXECUTABLE_UNION(AtmKind.EXECUTABLE, AtmKind.UNION),
  EXECUTABLE_WILDCARD(AtmKind.EXECUTABLE, AtmKind.WILDCARD),

  INTERSECTION_ARRAY(AtmKind.INTERSECTION, AtmKind.ARRAY),
  INTERSECTION_DECLARED(AtmKind.INTERSECTION, AtmKind.DECLARED),
  INTERSECTION_EXECUTABLE(AtmKind.INTERSECTION, AtmKind.EXECUTABLE),
  INTERSECTION_INTERSECTION(AtmKind.INTERSECTION, AtmKind.INTERSECTION),
  INTERSECTION_NONE(AtmKind.INTERSECTION, AtmKind.NONE),
  INTERSECTION_NULL(AtmKind.INTERSECTION, AtmKind.NULL),
  INTERSECTION_PRIMITIVE(AtmKind.INTERSECTION, AtmKind.PRIMITIVE),
  INTERSECTION_TYPEVAR(AtmKind.INTERSECTION, AtmKind.TYPEVAR),
  INTERSECTION_UNION(AtmKind.INTERSECTION, AtmKind.UNION),
  INTERSECTION_WILDCARD(AtmKind.INTERSECTION, AtmKind.WILDCARD),

  NONE_ARRAY(AtmKind.NONE, AtmKind.ARRAY),
  NONE_DECLARED(AtmKind.NONE, AtmKind.DECLARED),
  NONE_EXECUTABLE(AtmKind.NONE, AtmKind.EXECUTABLE),
  NONE_INTERSECTION(AtmKind.NONE, AtmKind.INTERSECTION),
  NONE_NONE(AtmKind.NONE, AtmKind.NONE),
  NONE_NULL(AtmKind.NONE, AtmKind.NULL),
  NONE_PRIMITIVE(AtmKind.NONE, AtmKind.PRIMITIVE),
  NONE_TYPEVAR(AtmKind.NONE, AtmKind.TYPEVAR),
  NONE_UNION(AtmKind.NONE, AtmKind.UNION),
  NONE_WILDCARD(AtmKind.NONE, AtmKind.WILDCARD),

  NULL_ARRAY(AtmKind.NULL, AtmKind.ARRAY),
  NULL_DECLARED(AtmKind.NULL, AtmKind.DECLARED),
  NULL_EXECUTABLE(AtmKind.NULL, AtmKind.EXECUTABLE),
  NULL_INTERSECTION(AtmKind.NULL, AtmKind.INTERSECTION),
  NULL_NONE(AtmKind.NULL, AtmKind.NONE),
  NULL_NULL(AtmKind.NULL, AtmKind.NULL),
  NULL_PRIMITIVE(AtmKind.NULL, AtmKind.PRIMITIVE),
  NULL_TYPEVAR(AtmKind.NULL, AtmKind.TYPEVAR),
  NULL_UNION(AtmKind.NULL, AtmKind.UNION),
  NULL_WILDCARD(AtmKind.NULL, AtmKind.WILDCARD),

  PRIMITIVE_ARRAY(AtmKind.PRIMITIVE, AtmKind.ARRAY),
  PRIMITIVE_DECLARED(AtmKind.PRIMITIVE, AtmKind.DECLARED),
  PRIMITIVE_EXECUTABLE(AtmKind.PRIMITIVE, AtmKind.EXECUTABLE),
  PRIMITIVE_INTERSECTION(AtmKind.PRIMITIVE, AtmKind.INTERSECTION),
  PRIMITIVE_NONE(AtmKind.PRIMITIVE, AtmKind.NONE),
  PRIMITIVE_NULL(AtmKind.PRIMITIVE, AtmKind.NULL),
  PRIMITIVE_PRIMITIVE(AtmKind.PRIMITIVE, AtmKind.PRIMITIVE),
  PRIMITIVE_TYPEVAR(AtmKind.PRIMITIVE, AtmKind.TYPEVAR),
  PRIMITIVE_UNION(AtmKind.PRIMITIVE, AtmKind.UNION),
  PRIMITIVE_WILDCARD(AtmKind.PRIMITIVE, AtmKind.WILDCARD),

  TYPEVAR_ARRAY(AtmKind.TYPEVAR, AtmKind.ARRAY),
  TYPEVAR_DECLARED(AtmKind.TYPEVAR, AtmKind.DECLARED),
  TYPEVAR_EXECUTABLE(AtmKind.TYPEVAR, AtmKind.EXECUTABLE),
  TYPEVAR_INTERSECTION(AtmKind.TYPEVAR, AtmKind.INTERSECTION),
  TYPEVAR_NONE(AtmKind.TYPEVAR, AtmKind.NONE),
  TYPEVAR_NULL(AtmKind.TYPEVAR, AtmKind.NULL),
  TYPEVAR_PRIMITIVE(AtmKind.TYPEVAR, AtmKind.PRIMITIVE),
  TYPEVAR_TYPEVAR(AtmKind.TYPEVAR, AtmKind.TYPEVAR),
  TYPEVAR_UNION(AtmKind.TYPEVAR, AtmKind.UNION),
  TYPEVAR_WILDCARD(AtmKind.TYPEVAR, AtmKind.WILDCARD),

  UNION_ARRAY(AtmKind.UNION, AtmKind.ARRAY),
  UNION_DECLARED(AtmKind.UNION, AtmKind.DECLARED),
  UNION_EXECUTABLE(AtmKind.UNION, AtmKind.EXECUTABLE),
  UNION_INTERSECTION(AtmKind.UNION, AtmKind.INTERSECTION),
  UNION_NONE(AtmKind.UNION, AtmKind.NONE),
  UNION_NULL(AtmKind.UNION, AtmKind.NULL),
  UNION_PRIMITIVE(AtmKind.UNION, AtmKind.PRIMITIVE),
  UNION_TYPEVAR(AtmKind.UNION, AtmKind.TYPEVAR),
  UNION_UNION(AtmKind.UNION, AtmKind.UNION),
  UNION_WILDCARD(AtmKind.UNION, AtmKind.WILDCARD),

  WILDCARD_ARRAY(AtmKind.WILDCARD, AtmKind.ARRAY),
  WILDCARD_DECLARED(AtmKind.WILDCARD, AtmKind.DECLARED),
  WILDCARD_EXECUTABLE(AtmKind.WILDCARD, AtmKind.EXECUTABLE),
  WILDCARD_INTERSECTION(AtmKind.WILDCARD, AtmKind.INTERSECTION),
  WILDCARD_NONE(AtmKind.WILDCARD, AtmKind.NONE),
  WILDCARD_NULL(AtmKind.WILDCARD, AtmKind.NULL),
  WILDCARD_PRIMITIVE(AtmKind.WILDCARD, AtmKind.PRIMITIVE),
  WILDCARD_TYPEVAR(AtmKind.WILDCARD, AtmKind.TYPEVAR),
  WILDCARD_UNION(AtmKind.WILDCARD, AtmKind.UNION),
  WILDCARD_WILDCARD(AtmKind.WILDCARD, AtmKind.WILDCARD);

  /** First AtmKind. */
  public final AtmKind type1Kind;
  /** Second AtmKind. */
  public final AtmKind type2Kind;

  /**
   * Creates an AtmCombo.
   *
   * @param type1Kind first kind
   * @param type2Kind second kind
   */
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
   * Returns the AtmCombo corresponding to the given ATM pair of the given ATMKinds. e.g. {@literal
   * (AtmKind.NULL, AtmKind.EXECUTABLE) => AtmCombo.NULL_EXECUTABLE}.
   *
   * @return the AtmCombo corresponding to the given ATM pair of the given ATMKinds. e.g. {@literal
   *     (AtmKind.NULL, AtmKind.EXECUTABLE) => AtmCombo.NULL_EXECUTABLE}
   */
  public static AtmCombo valueOf(final AtmKind type1, final AtmKind type2) {
    return comboMap[type1.ordinal()][type2.ordinal()];
  }

  /**
   * Returns the AtmCombo corresponding to the pair of the classes for the given
   * AnnotatedTypeMirrors. e.g. {@literal (AnnotatedPrimitiveType, AnnotatedDeclaredType) =>
   * AtmCombo.PRIMITIVE_DECLARED}
   *
   * @return the AtmCombo corresponding to the pair of the classes for the given
   *     AnnotatedTypeMirrors
   */
  public static AtmCombo valueOf(final AnnotatedTypeMirror type1, final AnnotatedTypeMirror type2) {
    return valueOf(AtmKind.valueOf(type1), AtmKind.valueOf(type2));
  }

  /**
   * Call the visit method that corresponds to the AtmCombo that represents the classes of type1 and
   * type2. That is, get the combo for type1 and type 2, use it to identify the correct visitor
   * method, and call that method with type1, type2, and initialParam as arguments to the visit
   * method.
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
            (AnnotatedArrayType) type1, (AnnotatedIntersectionType) type2, initialParam);

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
            (AnnotatedDeclaredType) type1, (AnnotatedExecutableType) type2, initialParam);

      case DECLARED_INTERSECTION:
        return visitor.visitDeclared_Intersection(
            (AnnotatedDeclaredType) type1, (AnnotatedIntersectionType) type2, initialParam);

      case DECLARED_NONE:
        return visitor.visitDeclared_None(
            (AnnotatedDeclaredType) type1, (AnnotatedNoType) type2, initialParam);

      case DECLARED_NULL:
        return visitor.visitDeclared_Null(
            (AnnotatedDeclaredType) type1, (AnnotatedNullType) type2, initialParam);

      case DECLARED_PRIMITIVE:
        return visitor.visitDeclared_Primitive(
            (AnnotatedDeclaredType) type1, (AnnotatedPrimitiveType) type2, initialParam);

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
            (AnnotatedExecutableType) type1, (AnnotatedDeclaredType) type2, initialParam);

      case EXECUTABLE_EXECUTABLE:
        return visitor.visitExecutable_Executable(
            (AnnotatedExecutableType) type1, (AnnotatedExecutableType) type2, initialParam);

      case EXECUTABLE_INTERSECTION:
        return visitor.visitExecutable_Intersection(
            (AnnotatedExecutableType) type1, (AnnotatedIntersectionType) type2, initialParam);

      case EXECUTABLE_NONE:
        return visitor.visitExecutable_None(
            (AnnotatedExecutableType) type1, (AnnotatedNoType) type2, initialParam);

      case EXECUTABLE_NULL:
        return visitor.visitExecutable_Null(
            (AnnotatedExecutableType) type1, (AnnotatedNullType) type2, initialParam);

      case EXECUTABLE_PRIMITIVE:
        return visitor.visitExecutable_Primitive(
            (AnnotatedExecutableType) type1, (AnnotatedPrimitiveType) type2, initialParam);

      case EXECUTABLE_TYPEVAR:
        return visitor.visitExecutable_Typevar(
            (AnnotatedExecutableType) type1, (AnnotatedTypeVariable) type2, initialParam);

      case EXECUTABLE_UNION:
        return visitor.visitExecutable_Union(
            (AnnotatedExecutableType) type1, (AnnotatedUnionType) type2, initialParam);

      case EXECUTABLE_WILDCARD:
        return visitor.visitExecutable_Wildcard(
            (AnnotatedExecutableType) type1, (AnnotatedWildcardType) type2, initialParam);

      case INTERSECTION_ARRAY:
        return visitor.visitIntersection_Array(
            (AnnotatedIntersectionType) type1, (AnnotatedArrayType) type2, initialParam);

      case INTERSECTION_DECLARED:
        return visitor.visitIntersection_Declared(
            (AnnotatedIntersectionType) type1, (AnnotatedDeclaredType) type2, initialParam);

      case INTERSECTION_EXECUTABLE:
        return visitor.visitIntersection_Executable(
            (AnnotatedIntersectionType) type1, (AnnotatedExecutableType) type2, initialParam);

      case INTERSECTION_INTERSECTION:
        return visitor.visitIntersection_Intersection(
            (AnnotatedIntersectionType) type1, (AnnotatedIntersectionType) type2, initialParam);

      case INTERSECTION_NONE:
        return visitor.visitIntersection_None(
            (AnnotatedIntersectionType) type1, (AnnotatedNoType) type2, initialParam);

      case INTERSECTION_NULL:
        return visitor.visitIntersection_Null(
            (AnnotatedIntersectionType) type1, (AnnotatedNullType) type2, initialParam);

      case INTERSECTION_PRIMITIVE:
        return visitor.visitIntersection_Primitive(
            (AnnotatedIntersectionType) type1, (AnnotatedPrimitiveType) type2, initialParam);

      case INTERSECTION_TYPEVAR:
        return visitor.visitIntersection_Typevar(
            (AnnotatedIntersectionType) type1, (AnnotatedTypeVariable) type2, initialParam);

      case INTERSECTION_UNION:
        return visitor.visitIntersection_Union(
            (AnnotatedIntersectionType) type1, (AnnotatedUnionType) type2, initialParam);

      case INTERSECTION_WILDCARD:
        return visitor.visitIntersection_Wildcard(
            (AnnotatedIntersectionType) type1, (AnnotatedWildcardType) type2, initialParam);

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
            (AnnotatedPrimitiveType) type1, (AnnotatedDeclaredType) type2, initialParam);

      case PRIMITIVE_EXECUTABLE:
        return visitor.visitPrimitive_Executable(
            (AnnotatedPrimitiveType) type1, (AnnotatedExecutableType) type2, initialParam);

      case PRIMITIVE_INTERSECTION:
        return visitor.visitPrimitive_Intersection(
            (AnnotatedPrimitiveType) type1, (AnnotatedIntersectionType) type2, initialParam);

      case PRIMITIVE_NONE:
        return visitor.visitPrimitive_None(
            (AnnotatedPrimitiveType) type1, (AnnotatedNoType) type2, initialParam);

      case PRIMITIVE_NULL:
        return visitor.visitPrimitive_Null(
            (AnnotatedPrimitiveType) type1, (AnnotatedNullType) type2, initialParam);

      case PRIMITIVE_PRIMITIVE:
        return visitor.visitPrimitive_Primitive(
            (AnnotatedPrimitiveType) type1, (AnnotatedPrimitiveType) type2, initialParam);

      case PRIMITIVE_TYPEVAR:
        return visitor.visitPrimitive_Typevar(
            (AnnotatedPrimitiveType) type1, (AnnotatedTypeVariable) type2, initialParam);

      case PRIMITIVE_UNION:
        return visitor.visitPrimitive_Union(
            (AnnotatedPrimitiveType) type1, (AnnotatedUnionType) type2, initialParam);

      case PRIMITIVE_WILDCARD:
        return visitor.visitPrimitive_Wildcard(
            (AnnotatedPrimitiveType) type1, (AnnotatedWildcardType) type2, initialParam);

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
            (AnnotatedUnionType) type1, (AnnotatedIntersectionType) type2, initialParam);

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
            (AnnotatedTypeVariable) type1, (AnnotatedExecutableType) type2, initialParam);

      case TYPEVAR_INTERSECTION:
        return visitor.visitTypevar_Intersection(
            (AnnotatedTypeVariable) type1, (AnnotatedIntersectionType) type2, initialParam);

      case TYPEVAR_NONE:
        return visitor.visitTypevar_None(
            (AnnotatedTypeVariable) type1, (AnnotatedNoType) type2, initialParam);

      case TYPEVAR_NULL:
        return visitor.visitTypevar_Null(
            (AnnotatedTypeVariable) type1, (AnnotatedNullType) type2, initialParam);

      case TYPEVAR_PRIMITIVE:
        return visitor.visitTypevar_Primitive(
            (AnnotatedTypeVariable) type1, (AnnotatedPrimitiveType) type2, initialParam);

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
            (AnnotatedWildcardType) type1, (AnnotatedExecutableType) type2, initialParam);

      case WILDCARD_INTERSECTION:
        return visitor.visitWildcard_Intersection(
            (AnnotatedWildcardType) type1, (AnnotatedIntersectionType) type2, initialParam);

      case WILDCARD_NONE:
        return visitor.visitWildcard_None(
            (AnnotatedWildcardType) type1, (AnnotatedNoType) type2, initialParam);

      case WILDCARD_NULL:
        return visitor.visitWildcard_Null(
            (AnnotatedWildcardType) type1, (AnnotatedNullType) type2, initialParam);

      case WILDCARD_PRIMITIVE:
        return visitor.visitWildcard_Primitive(
            (AnnotatedWildcardType) type1, (AnnotatedPrimitiveType) type2, initialParam);

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
