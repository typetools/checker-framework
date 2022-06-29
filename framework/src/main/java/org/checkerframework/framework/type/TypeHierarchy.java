package org.checkerframework.framework.type;

import org.checkerframework.framework.util.AnnotatedTypes;

/** Compares AnnotatedTypeMirrors for subtype relationships. See also {@link QualifierHierarchy}. */
public interface TypeHierarchy {

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
   *   <li>Boxing conversions: isSubtype calls {@link AnnotatedTypes#asSuper( AnnotatedTypeFactory,
   *       AnnotatedTypeMirror, AnnotatedTypeMirror)} which calls {@link
   *       AnnotatedTypeFactory#getBoxedType}
   *   <li>Unboxing conversions: isSubtype calls {@link AnnotatedTypes#asSuper(
   *       AnnotatedTypeFactory, AnnotatedTypeMirror, AnnotatedTypeMirror)} which calls {@link
   *       AnnotatedTypeFactory#getUnboxedType}
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
   * @return true if {@code subtype} is a subtype of {@code supertype} for all hierarchies present.
   */
  boolean isSubtype(AnnotatedTypeMirror subtype, AnnotatedTypeMirror supertype);
}
