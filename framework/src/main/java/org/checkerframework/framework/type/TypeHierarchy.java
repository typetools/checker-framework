package org.checkerframework.framework.type;

import java.util.Collection;
import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.util.AnnotatedTypes;

/** Compares AnnotatedTypeMirrors for subtype relationships. See also {@link QualifierHierarchy}. */
public interface TypeHierarchy {

  // This can be used if:
  //  * the type is fully annotated,
  //  * the basetypes are Java subtypes, and
  //  * you want to check the full type
  // Otherwise, call QualifierHierarchy.

  // `TypeHierarchy` is an interface because the only implementation, DefaultTypeHierarchy, has
  // public visitor methods that clients should never call.

  /**
   * Returns true if {@code subtype} is a subtype of or convertible to {@code supertype} for all
   * hierarchies present. If the underlying Java type of {@code subtype} is not a subtype of or
   * convertible to the underlying Java type of {@code supertype}, then the behavior of this method
   * is undefined.
   *
   * <p>Ideally, types that require conversions would be converted before isSubtype is called, but
   * instead, isSubtype performs some of these conversions.
   *
   * <p>JLS 5.1 specifies 13 categories of conversions.
   *
   * <p>3 categories are converted in isSubtype:
   *
   * <ul>
   *   <li>Boxing conversions: isSubtype calls {@link AnnotatedTypeFactory#getBoxedType}
   *   <li>Unboxing conversions: isSubtype calls {@link AnnotatedTypeFactory#getUnboxedType}
   *   <li>String conversions: Any type to String. isSubtype calls {@link AnnotatedTypes#asSuper}
   *       which calls {@link AnnotatedTypeFactory#getStringType(AnnotatedTypeMirror)}
   * </ul>
   *
   * 2 happen elsewhere:
   *
   * <ul>
   *   <li>Unchecked conversions: Generic type to raw type. Raw types are instantiated with bounds
   *       in AnnotatedTypeFactory#fromTypeTree before is subtype is called
   *   <li>Capture conversions: Wildcards are captured in {@link
   *       AnnotatedTypeFactory#applyCaptureConversion(AnnotatedTypeMirror)}
   * </ul>
   *
   * 7 are not explicitly converted and are treated as though the types are actually subtypes.
   *
   * <ul>
   *   <li>Identity conversions: type to same type
   *   <li>Widening primitive conversions: primitive to primitive (no loss of information, byte to
   *       short for example)
   *   <li>Narrowing primitive conversions: primitive to primitive (possibly loss of information,
   *       short to byte for example)
   *   <li>Widening and Narrowing Primitive Conversion: byte to char
   *   <li>Widening reference conversions: Upcast
   *   <li>Narrowing reference conversions: Downcast
   *   <li>Value set conversions: floating-point value from one value set to another without
   *       changing its type.
   * </ul>
   *
   * @param subtype possible subtype
   * @param supertype possible supertype
   * @return true if {@code subtype} is a subtype of {@code supertype} for all hierarchies present
   */
  boolean isSubtype(AnnotatedTypeMirror subtype, AnnotatedTypeMirror supertype);

  /**
   * Returns true if the effective annotations of {@code subtype} are equal to or are sub-qualifiers
   * of the effective annotations of {@code supertype}, according to the type qualifier hierarchy.
   *
   * <p>The underlying types of {@code subtype} and {@code supertype} are not necessarily in a Java
   * subtyping relationship with one another and are only used by this method for special cases when
   * qualifier subtyping depends on the Java basetype.
   *
   * @param subtype possible subtype
   * @param supertype possible supertype
   * @return true iff the effective annotations of {@code subtype} are equal to or are
   *     sub-qualifiers of the effective annotations of {@code supertype}
   */
  boolean isSubtypeShallowEffective(AnnotatedTypeMirror subtype, AnnotatedTypeMirror supertype);

  /**
   * Returns true if the effective annotation in the same hierarchy as {@code hierarchy} of {@code
   * subtype} are equal to or are sub-qualifiers of the effective annotation of {@code supertype} in
   * the same hierarchy as {@code hierarchy}, according to the type qualifier hierarchy. Other
   * annotations in {@code subtype} and {@code supertype} are ignored.
   *
   * <p>The underlying types of {@code subtype} and {@code supertype} are not necessarily in a Java
   * subtyping relationship with one another and are only used by this method for special cases when
   * qualifier subtyping depends on the Java basetype.
   *
   * @param subtype possible subtype
   * @param supertype possible supertype
   * @param hierarchy an annotation whose hierarchy is used to compare {@code subtype} and {@code
   *     supertype}
   * @return true iff the effective annotation in the same hierarchy as {@code hierarchy} of {@code
   *     subtype} are equal to or are sub-qualifiers of the effective annotation of {@code
   *     supertype} in the same hierarchy as {@code hierarchy}
   */
  boolean isSubtypeShallowEffective(
      AnnotatedTypeMirror subtype, AnnotatedTypeMirror supertype, AnnotationMirror hierarchy);

  /**
   * Returns true if the effective annotations of {@code subtype} are equal to or are sub-qualifiers
   * of {@code superQualifiers}, according to the type qualifier hierarchy. Other annotations in
   * {@code subtype} are ignored.
   *
   * <p>The underlying type of {@code subtype} is only used by this method for special cases when
   * qualifier subtyping depends on the Java basetype.
   *
   * @param subtype possible subtype
   * @param superQualifiers possible superQualifiers
   * @return true iff the effective annotations of {@code subtype} are equal to or are
   *     sub-qualifiers of {@code superQualifiers}
   */
  boolean isSubtypeShallowEffective(
      AnnotatedTypeMirror subtype, Collection<? extends AnnotationMirror> superQualifiers);

  /**
   * Returns true if {@code subQualifiers} are equal to or are sub-qualifiers of the effective
   * annotations of {@code supertype}, according to the type qualifier hierarchy. Other annotations
   * in {@code supertype} are ignored.
   *
   * <p>The underlying type of {@code supertype} is used by this method for special cases when
   * qualifier subtyping depends on the Java basetype.
   *
   * @param subQualifiers possible subQualifiers
   * @param supertype possible supertype
   * @return true iff {@code subQualifiers} are equal to or are sub-qualifiers of the effective
   *     annotations of {@code supertype}
   */
  boolean isSubtypeShallowEffective(
      Collection<? extends AnnotationMirror> subQualifiers, AnnotatedTypeMirror supertype);

  /**
   * Returns true if the effective annotation of {@code subtype} in the same hierarchy as {@code
   * superQualifier} is equal to or sub-qualifier of {@code superQualifier}, according to the type
   * qualifier hierarchy. The underlying types of {@code subtype} is only used by this method for
   * special cases when qualifier subtyping depends on the Java basetype. Other annotations in
   * {@code subtype} are ignored.
   *
   * @param subtype possible subtype
   * @param superQualifier possible super qualifier
   * @return true iffhe effective annotation of {@code subtype} in the same hierarchy as {@code
   *     superQualifier} is equal to or sub-qualifier of {@code superQualifier}
   */
  boolean isSubtypeShallowEffective(AnnotatedTypeMirror subtype, AnnotationMirror superQualifier);

  /**
   * Returns true if {@code subQualifier} is equal to or sub-qualifier of the effective annotation
   * of {@code supertype} in the same hierarchy as {@code subQualifier} according to the type
   * qualifier hierarchy. The underlying types of {@code supertype} is only used by this method for
   * special cases when qualifier subtyping depends on the Java basetype. Other annotations in
   * {@code supertype} are ignored.
   *
   * @param subQualifier possible subQualifier
   * @param supertype possible supertype
   * @return true {@code subQualifier} is equal to or sub-qualifier of the effective annotation of
   *     {@code supertype} in the same hierarchy as {@code subQualifier}
   */
  boolean isSubtypeShallowEffective(AnnotationMirror subQualifier, AnnotatedTypeMirror supertype);

  /**
   * Returns a list of the indices of the type arguments that are covariant.
   *
   * @param type a type
   * @return a list of the indices of the type arguments that are covariant
   */
  List<Integer> getCovariantArgIndexes(AnnotatedDeclaredType type);
}
